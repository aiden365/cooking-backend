package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.UserDishCollectEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.DishService;
import com.cooking.core.service.UserDishCollectService;
import com.cooking.core.service.UserService;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * User dish collect controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/userDishCollectEntity")
public class UserDishCollectApi extends BaseController {

    @Autowired
    private UserDishCollectService userDishCollectService;
    @Autowired
    private UserService userService;
    @Autowired
    private DishService dishService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        int pageNo = params.getIntValue("pageNo");
        int pageSize = params.getIntValue("pageSize");
        IPage<UserDishCollectEntity> page = new Page<>(pageNo, pageSize);

        String search = params.getString("search");
        String userId = params.getString("userId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("userId", userId);
        IPage<UserDishCollectEntity> entityIPage = userDishCollectService.findPage(page, queryParams);

        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(UserDishCollectEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, DishEntity> dishEntityMap = dishService.findMapByIds(entityIPage.getRecords().stream().map(UserDishCollectEntity::getDishId).collect(Collectors.toSet()));
        entityIPage.getRecords().forEach(e -> {
            UserEntity userEntity = userEntityMap.get(e.getUserId());
            DishEntity dishEntity = dishEntityMap.get(e.getDishId());
            e.setUserName(userEntity == null ? "" : userEntity.getUserName());
            e.setDishName(dishEntity == null ? "" : dishEntity.getName());
        });

        return ok(entityIPage);
    }

    @PostMapping("add")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse add(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        Long dishId = params.getLong("dishId");

        UserEntity userEntity = validateUser(userId);
        DishEntity dishEntity = validateDish(dishId);

        LambdaQueryWrapper<UserDishCollectEntity> wrapper = new LambdaQueryWrapper<UserDishCollectEntity>()
                .eq(UserDishCollectEntity::getUserId, userId)
                .eq(UserDishCollectEntity::getDishId, dishId);
        UserDishCollectEntity collectEntity = userDishCollectService.getOne(wrapper);
        if (collectEntity != null) {
            return ok(collectEntity);
        }

        collectEntity = new UserDishCollectEntity()
                .setUserId(userEntity.getId())
                .setDishId(dishEntity.getId());
        userDishCollectService.save(collectEntity);

        Integer popularVal = dishEntity.getPopularVal();
        dishEntity.setPopularVal((popularVal == null ? 0 : popularVal) + 1);
        dishService.updateById(dishEntity);

        return ok(collectEntity);
    }

    @PostMapping("delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        Long dishId = params.getLong("dishId");

        validateUser(userId);
        DishEntity dishEntity = validateDish(dishId);

        LambdaQueryWrapper<UserDishCollectEntity> wrapper = new LambdaQueryWrapper<UserDishCollectEntity>()
                .eq(UserDishCollectEntity::getUserId, userId)
                .eq(UserDishCollectEntity::getDishId, dishId);
        UserDishCollectEntity collectEntity = userDishCollectService.getOne(wrapper);
        if (collectEntity == null) {
            return ok();
        }

        userDishCollectService.removeById(collectEntity.getId());

        int popularVal = dishEntity.getPopularVal() == null ? 0 : dishEntity.getPopularVal();
        dishEntity.setPopularVal(Math.max(0, popularVal - 1));
        dishService.updateById(dishEntity);

        return ok();
    }

    private UserEntity validateUser(Long userId) {
        if (userId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "userId不能为空");
        }
        UserEntity userEntity = userService.getById(userId);
        if (userEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "用户不存在");
        }
        return userEntity;
    }

    private DishEntity validateDish(Long dishId) {
        if (dishId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        DishEntity dishEntity = dishService.getById(dishId);
        if (dishEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品不存在");
        }
        return dishEntity;
    }
}
