package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.*;
import com.cooking.core.service.DishService;
import com.cooking.core.service.UserService;
import com.cooking.core.service.UserShareCommentService;
import com.cooking.core.service.UserShareService;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.SystemContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * User share comment controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@RestController
@RequestMapping("userShareComment")
public class UserShareCommentApi extends BaseController {

    private static final int CONTENT_MAX_LENGTH = 255;

    @Autowired
    private UserShareCommentService userShareCommentService;
    @Autowired
    private UserService userService;
    @Autowired
    private DishService dishService;
    @Autowired
    private UserShareService userShareService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        IPage<UserShareCommentEntity> entityIPage = query(new Page<>(pageNo, pageSize), params);
        return ok(entityIPage);
    }

    @PostMapping("list")
    public BaseResponse list(@RequestBody JSONObject params) {
        return ok(query(new Page<>(1, -1), params).getRecords());
    }

    private IPage<UserShareCommentEntity> query(IPage<UserShareCommentEntity> page, Map<String, Object> params){
        params.putIfAbsent("parentId", 0L);

        IPage<UserShareCommentEntity> entityIPage = userShareCommentService.findPage(page, params);
        //获取分享的子评论
        Set<Long> commentIds = entityIPage.getRecords().stream().map(BaseEntity::getId).collect(Collectors.toSet());
        List<UserShareCommentEntity> childCommentList = userShareCommentService.lambdaQuery().in(UserShareCommentEntity::getParentId, commentIds).list();
        Map<Long, List<UserShareCommentEntity>> childCommentListMap = childCommentList.stream().collect(Collectors.groupingBy(UserShareCommentEntity::getParentId));

        Set<Long> entityUserIds = entityIPage.getRecords().stream().map(UserShareCommentEntity::getUserId).collect(Collectors.toSet());
        entityUserIds.addAll(childCommentList.stream().map(UserShareCommentEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityUserIds);
        entityIPage.getRecords().forEach(e -> e.setUserName(userEntityMap.get(e.getUserId()).getUserName()));
        childCommentList.forEach(e -> e.setUserName(userEntityMap.get(e.getUserId()).getUserName()));

        entityIPage.getRecords().forEach(e -> e.setChildCommentList(childCommentListMap.get(e.getId())));

        return entityIPage;
    }

    @PostMapping("add")
    public BaseResponse add(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        String content = params.getString("content");
        Long parentId = params.getLong("parentId");
        Long shareId = params.getLong("shareId");
        if (parentId == null) {
            parentId = 0L;
        }

        if(userId == null){
            UserEntity currentUser = SystemContextHelper.getCurrentUser();
            userId = currentUser.getId();
        }

        UserEntity userEntity = validateUser(userId);
        validateShare(shareId);
        validateContent(content);

        if (parentId > 0) {
            UserShareCommentEntity parentComment = userShareCommentService.getById(parentId);
            if (parentComment == null) {
                throw new ApiException(BaseResponse.Code.fail.code, "上级评论不存在");
            }
        }

        UserShareCommentEntity commentEntity = new UserShareCommentEntity();
        commentEntity.setUserId(userEntity.getId());
        commentEntity.setUserShareId(shareId);
        commentEntity.setParentId(parentId);
        commentEntity.setContent(content.trim());
        userShareCommentService.save(commentEntity);
        return ok(commentEntity);
    }

    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long commentId = params.getLong("commentId");
        if (commentId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }

        userShareCommentService.removeById(commentId);
        return ok();
    }


    @PostMapping("start")
    public BaseResponse start(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }
        UserShareCommentEntity commentEntity = userShareCommentService.getById(id);
        if (commentEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "评论不存在");
        }

        userShareCommentService.incrementStartCount(id);
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

    private UserShareEntity validateShare(Long dishId) {
        if (dishId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        UserShareEntity userShareEntity = userShareService.getById(dishId);
        if (userShareEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品分享不存在");
        }
        return userShareEntity;
    }

    private void validateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new ApiException(BaseResponse.Code.fail.code, "评论内容不能为空");
        }
        if (content.trim().length() > CONTENT_MAX_LENGTH) {
            throw new ApiException(BaseResponse.Code.fail.code, "评论内容长度不能超过" + CONTENT_MAX_LENGTH);
        }
    }
}
