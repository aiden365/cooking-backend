package com.cooking.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishCommentEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.DishCommentService;
import com.cooking.core.service.UserService;
import com.cooking.utils.SystemContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Dish comment controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/dishComment/comment")
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
        Set<Long> entityIds = entityIPage.getRecords().stream().map(BaseEntity::getId).collect(Collectors.toSet());
        List<DishCommentEntity> childCommentList = dishCommentService.lambdaQuery().in(DishCommentEntity::getParentId, entityIds).list();
        Map<Long, List<DishCommentEntity>> childCommentListMap = childCommentList.stream().collect(Collectors.groupingBy(DishCommentEntity::getParentId));
        entityIPage.getRecords().forEach(e -> e.setChildCommentList(childCommentListMap.get(e.getId())));

        Set<Long> entityUserIds = entityIPage.getRecords().stream().map(DishCommentEntity::getUserId).collect(Collectors.toSet());
        entityUserIds.addAll(childCommentList.stream().map(DishCommentEntity::getUserId).collect(Collectors.toSet()));

        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityUserIds);
        entityIPage.getRecords().forEach(e -> e.setUserName(userEntityMap.get(e.getUserId()).getUserName()));
        childCommentList.forEach(e -> e.setUserName(userEntityMap.get(e.getUserId()).getUserName()));
        return ok(entityIPage);
    }

    @PostMapping("comment")
    public BaseResponse comment(@RequestBody JSONObject params) {
        Long parentId = params.getLong("parentId");
        String content = params.getString("content");
        if(!BaseEntity.validId(parentId)){
            return fail("父级ID不能为空");
        }
        if(StrUtil.isBlank(content)){
            return fail("内容不能为空");
        }

        DishCommentEntity entity = new DishCommentEntity();
        entity.setParentId(parentId);
        entity.setUserId(SystemContextHelper.getCurrentUser().getId());
        entity.setContent(content);
        dishCommentService.save(entity);
        return ok(entity);
    }
}
