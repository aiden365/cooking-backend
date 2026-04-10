package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.UserDietRecordEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.DishService;
import com.cooking.core.service.UserDietRecordService;
import com.cooking.core.service.UserService;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * User diet record controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("diet")
public class UserDietRecordApi extends BaseController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private UserDietRecordService userDietRecordService;
    @Autowired
    private UserService userService;
    @Autowired
    private DishService dishService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        IPage<UserDietRecordEntity> entityIPage = userDietRecordService.findPage(new Page<>(pageNo, pageSize), params);
        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(UserDietRecordEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, DishEntity> dishEntityMap = dishService.findMapByIds(entityIPage.getRecords().stream().map(UserDietRecordEntity::getDishId).collect(Collectors.toSet()));
        entityIPage.getRecords().forEach(e -> {
            UserEntity userEntity = userEntityMap.get(e.getUserId());
            DishEntity dishEntity = dishEntityMap.get(e.getDishId());
            e.setUserName(userEntity == null ? "" : userEntity.getUserName());
            e.setDishName(dishEntity == null ? "" : dishEntity.getName());
        });

        return ok(entityIPage);
    }

    @PostMapping("save")
    public BaseResponse save(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        Long dishId = params.getLong("dishId");
        String dietDate = params.getString("dietDate");
        Integer dietOrder = params.getInteger("dietOrder");

        validateParams(userId, dishId, dietDate, dietOrder);

        LambdaQueryWrapper<UserDietRecordEntity> wrapper = new LambdaQueryWrapper<UserDietRecordEntity>()
                .eq(UserDietRecordEntity::getUserId, userId)
                .eq(UserDietRecordEntity::getDietDate, dietDate)
                .eq(UserDietRecordEntity::getDietOrder, dietOrder);
        UserDietRecordEntity entity = userDietRecordService.getOne(wrapper);
        if (entity == null) {
            entity = new UserDietRecordEntity();
            entity.setUserId(userId);
            entity.setDietDate(dietDate);
            entity.setDietOrder(dietOrder);
        }
        entity.setDishId(dishId);
        userDietRecordService.saveOrUpdate(entity);
        return ok(entity);
    }

    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long dietId = params.getLong("dietId");
        if (dietId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }
        userDietRecordService.removeById(dietId);
        return ok();
    }



    private void validateParams(Long userId, Long dishId, String dietDate, Integer dietOrder) {
        if (userId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "userId不能为空");
        }
        if (dishId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        if (!StringUtils.hasText(dietDate)) {
            throw new ApiException(BaseResponse.Code.fail.code, "dietDate不能为空");
        }
        try {
            LocalDate.parse(dietDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ApiException(BaseResponse.Code.fail.code, "dietDate格式必须为yyyy-MM-dd");
        }
        if (dietOrder == null || dietOrder < 1 || dietOrder > 3) {
            throw new ApiException(BaseResponse.Code.fail.code, "dietOrder仅支持1-3");
        }

        if (userService.getById(userId) == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "用户不存在");
        }
        if (dishService.getById(dishId) == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品不存在");
        }
    }
}
