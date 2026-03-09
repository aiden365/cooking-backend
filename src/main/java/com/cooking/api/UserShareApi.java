package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.entity.UserShareEntity;
import com.cooking.core.service.DishService;
import com.cooking.core.service.UserService;
import com.cooking.core.service.UserShareService;
import com.cooking.enums.PathEnum;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * User share controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/userShare")
public class UserShareApi extends BaseController {

    private static final int DESCRIPTION_MAX_LENGTH = 500;

    @Autowired
    private UserShareService userShareService;
    @Autowired
    private UserService userService;
    @Autowired
    private DishService dishService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        String search = params.getString("search");
        Long userId = params.getLong("userId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("userId", userId);
        IPage<UserShareEntity> entityIPage = userShareService.findPage(new Page<>(pageNo, pageSize), queryParams);
        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(UserShareEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, DishEntity> dishEntityMap = dishService.findMapByIds(entityIPage.getRecords().stream().map(UserShareEntity::getDishId).collect(Collectors.toSet()));
        entityIPage.getRecords().forEach(e -> {
            UserEntity userEntity = userEntityMap.get(e.getUserId());
            DishEntity dishEntity = dishEntityMap.get(e.getDishId());
            e.setUserName(userEntity == null ? "" : userEntity.getUserName());
            e.setDishName(dishEntity == null ? "" : dishEntity.getName());
        });

        return ok(entityIPage);
    }

    @PostMapping(value = "add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse add(@RequestParam(value = "userId", required = false) Long userId,
                            @RequestParam(value = "dishId", required = false) Long dishId,
                            @RequestParam(value = "description", required = false) String description,
                            @RequestPart(value = "dishImg", required = false) MultipartFile dishImg) {

        UserEntity userEntity = validateUser(userId);
        DishEntity dishEntity = validateDish(dishId);
        validateDescription(description);
        if (dishImg == null || dishImg.isEmpty()) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品图片不能为空");
        }

        String relativePath = saveShareImage(userId, dishId, dishImg);

        UserShareEntity userShareEntity = new UserShareEntity();
        userShareEntity.setUserId(userEntity.getId());
        userShareEntity.setDishId(dishEntity.getId());
        userShareEntity.setDishImg(relativePath);
        userShareEntity.setDescription(description.trim());
        userShareService.save(userShareEntity);

        Integer activeVal = dishEntity.getActiveVal();
        dishEntity.setActiveVal((activeVal == null ? 0 : activeVal) + 1);
        dishService.updateById(dishEntity);

        return ok(userShareEntity);
    }

    @PostMapping("delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }

        UserShareEntity userShareEntity = userShareService.getById(id);
        if (userShareEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "分享记录不存在");
        }

        DishEntity dishEntity = validateDish(userShareEntity.getDishId());

        userShareService.removeById(id);
        deleteShareImage(userShareEntity.getDishImg());

        int activeVal = dishEntity.getActiveVal() == null ? 0 : dishEntity.getActiveVal();
        dishEntity.setActiveVal(Math.max(0, activeVal - 1));
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

    private void validateDescription(String description) {
        if (!StringUtils.hasText(description)) {
            throw new ApiException(BaseResponse.Code.fail.code, "分享描述不能为空");
        }
        if (description.trim().length() > DESCRIPTION_MAX_LENGTH) {
            throw new ApiException(BaseResponse.Code.fail.code, "分享描述长度不能超过" + DESCRIPTION_MAX_LENGTH);
        }
    }

    private String saveShareImage(Long userId, Long dishId, MultipartFile file) {
        String basePath = PathEnum.user_share_path.getValue().replaceFirst("^[/\\\\]+", "");
        Path shareDir = Paths.get(uploadPath, basePath, String.valueOf(userId), String.valueOf(dishId));
        try {
            Files.createDirectories(shareDir);
        } catch (IOException e) {
            throw new ApiException(BaseResponse.Code.fail.code, "创建分享图片目录失败");
        }

        String ext = "";
        String originFileName = file.getOriginalFilename();
        if (StringUtils.hasText(originFileName) && originFileName.lastIndexOf('.') >= 0) {
            ext = originFileName.substring(originFileName.lastIndexOf('.'));
        }

        String fileName = UUID.randomUUID() + ext;
        Path target = shareDir.resolve(fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new ApiException(BaseResponse.Code.fail.code, "保存分享图片失败");
        }

        return PathEnum.user_share_path.getValue() + "/" + userId + "/" + dishId + "/" + fileName;
    }

    private void deleteShareImage(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        String normalizedRelativePath = relativePath.replaceFirst("^[/\\\\]+", "");
        Path filePath = Paths.get(uploadPath, normalizedRelativePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new ApiException(BaseResponse.Code.fail.code, "删除分享图片失败");
        }
    }
}
