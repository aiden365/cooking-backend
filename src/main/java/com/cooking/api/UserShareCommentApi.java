package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.entity.UserShareCommentEntity;
import com.cooking.core.entity.UserShareEntity;
import com.cooking.core.service.DishService;
import com.cooking.core.service.UserService;
import com.cooking.core.service.UserShareCommentService;
import com.cooking.core.service.UserShareService;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * User share comment controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
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
        String search = params.getString("search");
        Long userShareId = params.getLong("userShareId");
        Long userId = params.getLong("userId");
        Long parentId = params.getLong("parentId");

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("userShareId", userShareId);
        queryParams.put("userId", userId);
        queryParams.put("parentId", parentId);

        IPage<UserShareCommentEntity> entityIPage = userShareCommentService.findPage(new Page<>(pageNo, pageSize), queryParams);
        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(UserShareCommentEntity::getUserId).collect(Collectors.toSet()));
        entityIPage.getRecords().forEach(e -> {
            UserEntity userEntity = userEntityMap.get(e.getUserId());
            e.setUserName(userEntity == null ? "" : userEntity.getUserName());
        });

        return ok(entityIPage);
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
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }
        UserShareCommentEntity commentEntity = userShareCommentService.getById(id);
        if (commentEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "评论不存在");
        }
        userShareCommentService.removeById(id);
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
