package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishCommentEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.DishCommentService;
import com.cooking.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Dish comment controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/dishCommentEntity")
public class DishCommentApi extends BaseController {

    @Autowired
    private DishCommentService dishCommentService;
    @Autowired
    private UserService userService;


    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        String search = params.getString("search");
        Long parentId = params.getLong("parentId");
        Long dishId = params.getLong("dishId");

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("parentId", parentId);
        queryParams.put("dishId", dishId);
        IPage<DishCommentEntity> entityIPage = dishCommentService.findPage(new Page<>(pageNo, pageSize), queryParams);
        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(DishCommentEntity::getUserId).collect(Collectors.toSet()));
        entityIPage.getRecords().forEach(e -> e.setUserName(userEntityMap.get(e.getUserId()).getUserName()));
        return ok(entityIPage);
    }
}
