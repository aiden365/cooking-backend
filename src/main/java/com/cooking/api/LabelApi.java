package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.DishLabelRelEntity;
import com.cooking.core.entity.LabelEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.entity.UserLabelRelEntity;
import com.cooking.core.service.DishLableRelService;
import com.cooking.core.service.DishService;
import com.cooking.core.service.LabelService;
import com.cooking.core.service.UserLabelRelService;
import com.cooking.core.service.UserService;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * <p>
 * Label controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@RestController
@RequestMapping("label")
public class LabelApi extends BaseController {

    @Autowired
    private LabelService labelService;
    @Autowired
    private UserLabelRelService userLabelRelService;
    @Autowired
    private DishLableRelService dishLableRelService;
    @Autowired
    private UserService userService;
    @Autowired
    private DishService dishService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        int pageNo = params.getIntValue("pageNo");
        int pageSize = params.getIntValue("pageSize");
        IPage<LabelEntity> entityIPage = labelService.findPage(new Page<>(pageNo, pageSize), params);
        return ok(entityIPage);
    }


    @PostMapping("save")
    public BaseResponse save(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        String labelName = params.getString("labelName");
        Integer type = params.getInteger("type");
        LabelEntity labelEntity = new LabelEntity();

        if (id != null) {
            labelEntity = labelService.getById(id);
            if (labelEntity == null) {
                throw new ApiException(BaseResponse.Code.fail.code, "标签不存在");
            }
        }

        LabelEntity sameNameLabel = labelService.lambdaQuery().eq(LabelEntity::getLabelName, labelName).one();
        if (sameNameLabel != null && !sameNameLabel.getId().equals(id)) {
            throw new ApiException(BaseResponse.Code.fail.code, "标签已存在");
        }

        if (labelName != null) {
            validateLabelName(labelName);
            labelEntity.setLabelName(labelName.trim());
        }
        if (type != null) {
            validateLabelType(type);
            labelEntity.setType(type);
        }
        labelService.saveOrUpdate(labelEntity);
        return ok(labelEntity);
    }

    @PostMapping("delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }


        userLabelRelService.remove(new LambdaQueryWrapper<UserLabelRelEntity>().eq(UserLabelRelEntity::getLabelId, id));
        dishLableRelService.remove(new LambdaQueryWrapper<DishLabelRelEntity>().eq(DishLabelRelEntity::getLabelId, id));
        labelService.removeById(id);
        return ok();
    }



    @PostMapping("relDish")
    public BaseResponse relDish(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        Long labelId = params.getLong("labelId");
        LabelEntity labelEntity = validateDishLabelRel(dishId, labelId);
        if (!Integer.valueOf(2).equals(labelEntity.getType())) {
            throw new ApiException(BaseResponse.Code.fail.code, "该标签不是菜品标签");
        }

        LambdaQueryWrapper<DishLabelRelEntity> wrapper = new LambdaQueryWrapper<DishLabelRelEntity>().eq(DishLabelRelEntity::getDishId, dishId).eq(DishLabelRelEntity::getLabelId, labelId);
        DishLabelRelEntity relEntity = dishLableRelService.getOne(wrapper);
        if (relEntity == null) {
            relEntity = new DishLabelRelEntity().setDishId(dishId).setLabelId(labelId);
            dishLableRelService.save(relEntity);
        }
        return ok(relEntity);
    }

    @PostMapping("unRelDish")
    public BaseResponse unRelDish(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        Long labelId = params.getLong("labelId");
        validateDishLabelRel(dishId, labelId);

        dishLableRelService.remove(new LambdaQueryWrapper<DishLabelRelEntity>().eq(DishLabelRelEntity::getDishId, dishId).eq(DishLabelRelEntity::getLabelId, labelId));
        return ok();
    }

    private LabelEntity validateUserLabelRel(Long userId, Long labelId) {
        if (userId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "userId不能为空");
        }
        if (labelId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "labelId不能为空");
        }
        UserEntity userEntity = userService.getById(userId);
        if (userEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "用户不存在");
        }
        LabelEntity labelEntity = labelService.getById(labelId);
        if (labelEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "标签不存在");
        }
        return labelEntity;
    }

    private LabelEntity validateDishLabelRel(Long dishId, Long labelId) {
        if (dishId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        if (labelId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "labelId不能为空");
        }
        DishEntity dishEntity = dishService.getById(dishId);
        if (dishEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品不存在");
        }
        LabelEntity labelEntity = labelService.getById(labelId);
        if (labelEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "标签不存在");
        }
        return labelEntity;
    }

    private void validateLabelName(String labelName) {
        if (!StringUtils.hasText(labelName)) {
            throw new ApiException(BaseResponse.Code.fail.code, "标签名不能为空");
        }
    }

    private void validateLabelType(Integer type) {
        if (type == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "标签类型不能为空");
        }
        if (!Integer.valueOf(1).equals(type) && !Integer.valueOf(2).equals(type)) {
            throw new ApiException(BaseResponse.Code.fail.code, "标签类型仅支持1或2");
        }
    }
}
