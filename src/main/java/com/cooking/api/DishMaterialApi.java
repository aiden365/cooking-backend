package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.DishMaterialEntity;
import com.cooking.core.service.DishMaterialService;
import com.cooking.core.service.DishService;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Dish material controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("dishMaterial")
public class DishMaterialApi extends BaseController {

    private static final int MATERIAL_NAME_MAX_LENGTH = 64;
    private static final int DOSAGE_MAX_LENGTH = 64;
    private static final int REMARK_MAX_LENGTH = 255;

    @Autowired
    private DishMaterialService dishMaterialService;
    @Autowired
    private DishService dishService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        String search = params.getString("search");
        Long dishId = params.getLong("dishId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("dishId", dishId);
        IPage<DishMaterialEntity> entityIPage = dishMaterialService.findPage(new Page<>(pageNo, pageSize), queryParams);
        return ok(entityIPage);
    }

    @PostMapping("add")
    public BaseResponse add(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        String materialName = params.getString("materialName");
        String dosage = params.getString("dosage");
        String remark = params.getString("remark");

        validateParams(dishId, materialName, dosage, remark);

        DishMaterialEntity dishMaterialEntity = new DishMaterialEntity();
        dishMaterialEntity.setDishId(dishId);
        dishMaterialEntity.setMaterialName(materialName.trim());
        dishMaterialEntity.setDosage(dosage.trim());
        dishMaterialEntity.setDeal(remark.trim());
        dishMaterialService.save(dishMaterialEntity);
        return ok(dishMaterialEntity);
    }

    @PostMapping("update")
    public BaseResponse update(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        Long dishId = params.getLong("dishId");
        String materialName = params.getString("materialName");
        String dosage = params.getString("dosage");
        String remark = params.getString("remark");

        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }
        DishMaterialEntity dishMaterialEntity = dishMaterialService.getById(id);
        if (dishMaterialEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "食材不存在");
        }

        validateParams(dishId, materialName, dosage, remark);

        dishMaterialEntity.setDishId(dishId);
        dishMaterialEntity.setMaterialName(materialName.trim());
        dishMaterialEntity.setDosage(dosage.trim());
        dishMaterialEntity.setDeal(remark.trim());
        dishMaterialService.updateById(dishMaterialEntity);
        return ok(dishMaterialEntity);
    }

    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }

        dishMaterialService.removeById(id);
        return ok();
    }

    private void validateParams(Long dishId, String materialName, String dosage, String remark) {
        if (dishId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        DishEntity dishEntity = dishService.getById(dishId);
        if (dishEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品不存在");
        }

        validateText(materialName, "食材名称", MATERIAL_NAME_MAX_LENGTH);
        validateText(dosage, "食材用量", DOSAGE_MAX_LENGTH);
        validateText(remark, "备注", REMARK_MAX_LENGTH);
    }

    private void validateText(String value, String fieldName, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(BaseResponse.Code.fail.code, fieldName + "不能为空");
        }
        if (value.trim().length() > maxLength) {
            throw new ApiException(BaseResponse.Code.fail.code, fieldName + "长度不能超过" + maxLength);
        }
    }
}
