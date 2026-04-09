package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishAppraisesEntity;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.DishAppraisesService;
import com.cooking.core.service.DishService;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.SystemContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Dish appraises controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("dishAppraises")
public class DishAppraisesApi extends BaseController {

    @Autowired
    private DishAppraisesService dishAppraisesService;
    @Autowired
    private DishService dishService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        int pageNo = params.getIntValue("pageNo");
        int pageSize = params.getIntValue("pageSize");

        String search = params.getString("search");
        Long dishId = params.getLong("dishId");
        Long userId = params.getLong("userId");

        IPage<DishAppraisesEntity> page = new Page<>(pageNo, pageSize);

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("dishId", dishId);
        queryParams.put("userId", userId);
        IPage<DishAppraisesEntity> entityIPage = dishAppraisesService.findPage(page, params);
        return ok(entityIPage);
    }

    @PostMapping("scoring")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse scoring(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        Integer manipulationScore = params.getInteger("manipulation_score");
        Integer equalScore = params.getInteger("equal_score");
        Integer satisfactionScore = params.getInteger("satisfaction_score");
        UserEntity currentUser = SystemContextHelper.getCurrentUser();

        if (ObjectUtils.isEmpty(dishId)) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        if (currentUser == null || currentUser.getId() == null) {
            throw new ApiException(BaseResponse.Code.user_wdl.code, "用户未登录");
        }
        validateScore("操作性评分", manipulationScore);
        validateScore("匹配度评分", equalScore);
        validateScore("满意度评分", satisfactionScore);

        DishEntity dishEntity = dishService.getById(dishId);
        if (dishEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品不存在");
        }

        LambdaQueryWrapper<DishAppraisesEntity> userDishWrapper = new LambdaQueryWrapper<DishAppraisesEntity>().eq(DishAppraisesEntity::getDishId, dishId).eq(DishAppraisesEntity::getUserId, currentUser.getId());
        DishAppraisesEntity dishAppraisesEntity = dishAppraisesService.getOne(userDishWrapper);
        if (dishAppraisesEntity == null) {
            dishAppraisesEntity = new DishAppraisesEntity();
            dishAppraisesEntity.setDishId(dishId);
            dishAppraisesEntity.setUserId(currentUser.getId());
            dishAppraisesEntity.setManipulationScore(manipulationScore);
            dishAppraisesEntity.setEqualScore(equalScore);
            dishAppraisesEntity.setSatisfactionScore(satisfactionScore);
            dishAppraisesService.save(dishAppraisesEntity);
        } else {
            dishAppraisesEntity.setManipulationScore(manipulationScore);
            dishAppraisesEntity.setEqualScore(equalScore);
            dishAppraisesEntity.setSatisfactionScore(satisfactionScore);
            dishAppraisesService.updateById(dishAppraisesEntity);
        }

        List<DishAppraisesEntity> appraisesList = dishAppraisesService.list(new LambdaQueryWrapper<DishAppraisesEntity>().eq(DishAppraisesEntity::getDishId, dishId));
        if (appraisesList.isEmpty()) {
            return ok();
        }

        double manipulationAvg = appraisesList.stream().mapToInt(DishAppraisesEntity::getManipulationScore).average().orElse(0D);
        double equalAvg = appraisesList.stream().mapToInt(DishAppraisesEntity::getEqualScore).average().orElse(0D);
        double satisfactionAvg = appraisesList.stream().mapToInt(DishAppraisesEntity::getSatisfactionScore).average().orElse(0D);
        int totalScore = buildTotalScore(manipulationAvg, equalAvg, satisfactionAvg);

        dishEntity.setActiveVal(appraisesList.size());
        dishEntity.setTotalScore(totalScore);
        dishService.updateById(dishEntity);

        Map<String, Object> result = new HashMap<>();
        result.put("dishId", dishId);
        result.put("manipulationAvg", manipulationAvg);
        result.put("equalAvg", equalAvg);
        result.put("satisfactionAvg", satisfactionAvg);
        result.put("totalScore", totalScore);
        return ok(result);
    }

    private void validateScore(String fieldName, Integer score) {
        if (score == null) {
            throw new ApiException(BaseResponse.Code.fail.code, fieldName + "不能为空");
        }
        if (score < 1 || score > 5) {
            throw new ApiException(BaseResponse.Code.fail.code, fieldName + "必须在1-5之间");
        }
    }

    private int buildTotalScore(double manipulationAvg, double equalAvg, double satisfactionAvg) {
        return BigDecimal.valueOf(manipulationAvg).multiply(BigDecimal.valueOf(0.3))
                .add(BigDecimal.valueOf(equalAvg).multiply(BigDecimal.valueOf(0.3)))
                .add(BigDecimal.valueOf(satisfactionAvg).multiply(BigDecimal.valueOf(0.4)))
                .multiply(BigDecimal.TEN)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}
