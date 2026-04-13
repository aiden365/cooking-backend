package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.*;
import com.cooking.core.service.DishAppraisesService;
import com.cooking.core.service.DishService;
import com.cooking.core.service.UserService;
import com.cooking.dto.DishScoreDTO;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Dish appraises controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@RestController
@RequestMapping("appraises")
public class DishAppraisesApi extends BaseController {

    @Autowired
    private DishAppraisesService dishAppraisesService;
    @Autowired
    private DishService dishService;
    @Autowired
    private UserService userService;

    @PostMapping("dishScorePage")
    public BaseResponse dishScorePage(@RequestBody JSONObject params) {
        String search = params.getString("search");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        IPage<DishScoreDTO> page = dishAppraisesService.findDishScorePage(new Page<>(pageNo, pageSize), queryParams);
        return ok(page);
    }

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        IPage<DishAppraisesEntity> entityIPage = dishAppraisesService.findPage(new Page<>(pageNo, pageSize), params);


        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(DishAppraisesEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, DishEntity> dishEntityMap = dishService.findMapByIds(entityIPage.getRecords().stream().map(DishAppraisesEntity::getDishId).collect(Collectors.toSet()));
        for (DishAppraisesEntity record : entityIPage.getRecords()) {
            UserEntity userEntity = userEntityMap.get(record.getUserId());
            DishEntity dishEntity = dishEntityMap.get(record.getDishId());
            record.setUserName(userEntity == null ? "" : userEntity.getUserName());
            record.setDishName(dishEntity == null ? "" : dishEntity.getName());
        }
        return ok(entityIPage);
    }


    /**
     * 菜品总分
     */
    @PostMapping("dish-total")
    public BaseResponse dishTotal(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        if (ObjectUtils.isEmpty(dishId)) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }

        DishEntity dishEntity = dishService.getById(dishId);
        if (dishEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品不存在");
        }

        List<DishAppraisesEntity> entityList = dishAppraisesService.lambdaQuery().eq(DishAppraisesEntity::getDishId, dishId).list();
        return ok(buildDishScoreResult(dishId, entityList));
    }

    /**
     * 用户对菜谱评价记录
     */
    @PostMapping("user-record")
    public BaseResponse userRecord(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        Long userId = currentUser.getId();

        DishAppraisesEntity dishAppraisesEntity = dishAppraisesService.lambdaQuery().eq(DishAppraisesEntity::getDishId, dishId).eq(DishAppraisesEntity::getUserId, userId).list().stream().findAny().orElse(null);
        JSONObject res = new JSONObject();
        res.put("manipulationScore", Optional.ofNullable(dishAppraisesEntity).map(DishAppraisesEntity::getManipulationScore).orElse(0));
        res.put("equalScore", Optional.ofNullable(dishAppraisesEntity).map(DishAppraisesEntity::getEqualScore).orElse(0));
        res.put("satisfactionScore", Optional.ofNullable(dishAppraisesEntity).map(DishAppraisesEntity::getSatisfactionScore).orElse(0));
        return ok(res);
    }

    @PostMapping("scoring")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse scoring(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        Integer manipulationScore = params.getInteger("manipulationScore");
        Integer equalScore = params.getInteger("equalScore");
        Integer satisfactionScore = params.getInteger("satisfactionScore");
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

        Map<String, Object> result = buildDishScoreResult(dishId, appraisesList);
        BigDecimal totalScore = (BigDecimal) result.get("totalScore");

        dishEntity.setActiveVal(appraisesList.size());
        dishEntity.setTotalScore(totalScore.setScale(0, RoundingMode.HALF_UP).doubleValue());
        dishService.updateById(dishEntity);

        return ok(result);
    }

    @PostMapping("delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }
        dishAppraisesService.removeById(id);
        return ok();
    }

    private void validateScore(String fieldName, Integer score) {
        if (score == null) {
            throw new ApiException(BaseResponse.Code.fail.code, fieldName + "不能为空");
        }
        if (score < 1 || score > 5) {
            throw new ApiException(BaseResponse.Code.fail.code, fieldName + "必须在1-5之间");
        }
    }

    private BigDecimal buildTotalScore(double manipulationAvg, double equalAvg, double satisfactionAvg) {
        return BigDecimal.valueOf(manipulationAvg).multiply(BigDecimal.valueOf(0.3))
                .add(BigDecimal.valueOf(equalAvg).multiply(BigDecimal.valueOf(0.3)))
                .add(BigDecimal.valueOf(satisfactionAvg).multiply(BigDecimal.valueOf(0.4)))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildDishScoreResult(Long dishId, List<DishAppraisesEntity> appraisesList) {
        double manipulationAvg = appraisesList.stream().mapToInt(DishAppraisesEntity::getManipulationScore).average().orElse(0D);
        double equalAvg = appraisesList.stream().mapToInt(DishAppraisesEntity::getEqualScore).average().orElse(0D);
        double satisfactionAvg = appraisesList.stream().mapToInt(DishAppraisesEntity::getSatisfactionScore).average().orElse(0D);
        BigDecimal totalScore = buildTotalScore(manipulationAvg, equalAvg, satisfactionAvg);

        Map<String, Object> result = new HashMap<>();
        result.put("dishId", dishId);
        result.put("manipulationAvg", manipulationAvg);
        result.put("equalAvg", equalAvg);
        result.put("satisfactionAvg", satisfactionAvg);
        result.put("totalScore", totalScore);
        result.put("appraiseCount", appraisesList.size());
        return result;
    }
}
