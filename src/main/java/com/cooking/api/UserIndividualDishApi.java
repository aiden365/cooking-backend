package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.*;
import com.cooking.core.service.DishService;
import com.cooking.core.service.LabelService;
import com.cooking.core.service.UserIndividualDishService;
import com.cooking.core.service.UserService;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cooking.base.BaseController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户个性化菜谱 前端控制器
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
@RestController
@RequestMapping("user/individual/dish")
public class UserIndividualDishApi extends BaseController {

    @Autowired
    private UserIndividualDishService userIndividualDishService;
    @Autowired
    private UserService userService;
    @Autowired
    private DishService dishService;
    @Autowired
    private LabelService labelService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        int pageNo = params.getIntValue("pageNo");
        int pageSize = params.getIntValue("pageSize");
        IPage<UserIndividualDishEntity> entityIPage = userIndividualDishService.findPage(new Page<>(pageNo, pageSize), params);

        Map<Long, DishEntity> dishEntityMap = dishService.findMapByIds(entityIPage.getRecords().stream().map(UserIndividualDishEntity::getDishId).collect(Collectors.toSet()));
        for (UserIndividualDishEntity entity : entityIPage.getRecords()) {
            DishEntity dishEntity = dishEntityMap.get(entity.getDishId());
            entity.setDishName(dishEntity == null ? "" : dishEntity.getName());
            entity.setDishImg(dishEntity == null ? "" : dishEntity.getImgPath());
        }

        return ok(entityIPage);
    }

    @PostMapping("aigc")
    public BaseResponse aigc(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        List<Long> lableIds = params.getList("lableIds", Long.class);

        return ok();
    }


    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }
        UserIndividualDishEntity entity = userIndividualDishService.getById(id);
        if (entity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "个性化菜谱不存在");
        }
        userIndividualDishService.removeById(id);
        return ok();
    }

}
