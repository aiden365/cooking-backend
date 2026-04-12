package com.cooking.api;

import cn.hutool.core.lang.Opt;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.NutritionEntity;
import com.cooking.core.entity.SystemParamsEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.entity.UserNutritionRelEntity;
import com.cooking.core.service.NutritionService;
import com.cooking.core.service.SystemParamsService;
import com.cooking.core.service.UserNutritionRelService;
import com.cooking.core.service.UserService;
import com.cooking.enums.SystemParamEnum;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.SystemContextHelper;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * User nutrition controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@RestController
@RequestMapping("userNutrition")
public class UserNutritionApi extends BaseController {

    @Autowired
    private UserNutritionRelService userNutritionRelService;
    @Autowired
    private NutritionService nutritionService;
    @Autowired
    private SystemParamsService systemParamsService;
    @Autowired
    private UserService userService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        String search = params.getString("search");
        Long userId = params.getLong("userId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("userId", userId);

        IPage<UserNutritionRelEntity> entityIPage = userNutritionRelService.findPage(new Page<>(pageNo, pageSize), queryParams);
        return ok(entityIPage);
    }


    @PostMapping("list")
    public BaseResponse list() {

        UserEntity currentUser = SystemContextHelper.getCurrentUser();

        List<UserNutritionRelEntity> relEntityList = userNutritionRelService.lambdaQuery().eq(UserNutritionRelEntity::getUserId, currentUser.getId()).list();
        Map<Long, NutritionEntity> nutritionEntityMap = nutritionService.findMapByIds(relEntityList.stream().map(UserNutritionRelEntity::getNutritionId).collect(Collectors.toSet()));
        List<JSONObject> list = new ArrayList<>();
        for (UserNutritionRelEntity relEntity : relEntityList) {

            String nutritionName = Optional.ofNullable(nutritionEntityMap.get(relEntity.getNutritionId())).map(NutritionEntity::getName).orElse(null);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", relEntity.getId());
            jsonObject.put("nutritionId", relEntity.getNutritionId());
            jsonObject.put("nutritionName", nutritionName);
            jsonObject.put("aimValue", relEntity.getValue());
            list.add(jsonObject);
        }
        return ok(list);
    }

    @PostMapping("save")
    public BaseResponse save(@RequestBody JSONObject params) {
        Long nutritionId = params.getLong("nutritionId");
        String value = params.getString("aimValue");

        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        Long userId = currentUser.getId();

        UserEntity userEntity = validateUser(userId);
        validateText(value, "营养目标值");

        if(!BaseEntity.validId(nutritionId)){
            throw new ApiException(BaseResponse.Code.fail.code, "营养元素不存在");
        }
        NutritionEntity nutrition = nutritionService.getById(nutritionId);
        if(nutrition == null){
            throw new ApiException(BaseResponse.Code.fail.code, "营养元素不存在");
        }

        UserNutritionRelEntity entity = userNutritionRelService.lambdaQuery().eq(UserNutritionRelEntity::getUserId, userId).eq(UserNutritionRelEntity::getNutritionId, nutritionId).one();
        if (entity == null) {
            checkNutritionLimit(userId);
            entity = UserNutritionRelEntity.builder().build();
        }
        // 不能添加重重复的营养
        UserNutritionRelEntity existRel = userNutritionRelService.lambdaQuery().eq(UserNutritionRelEntity::getUserId, userId).eq(UserNutritionRelEntity::getNutritionId, nutritionId).one();
        if (existRel != null && !existRel.getId().equals(entity.getId())) {
            throw new ApiException(BaseResponse.Code.fail.code, "用户已添加该营养目标");
        }

        entity.setUserId(userEntity.getId());
        entity.setNutritionId(nutritionId);
        entity.setValue(value.trim());
        userNutritionRelService.saveOrUpdate(entity);

        return ok(entity);
    }

    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }

        userNutritionRelService.removeById(id);
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

    private void validateText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(BaseResponse.Code.fail.code, fieldName + "不能为空");
        }
    }

    private void checkNutritionLimit(Long userId) {
        SystemParamsEntity limitParam = systemParamsService.findByName(SystemParamEnum.maxNutritionTargets);
        int limit;
        try {
            limit = Integer.parseInt(limitParam.getParamValue());
        } catch (Exception e) {
            limit = Integer.parseInt(SystemParamEnum.maxNutritionTargets.getValue());
        }
        long count = userNutritionRelService.lambdaQuery().eq(UserNutritionRelEntity::getUserId, userId).count();
        if (count >= limit) {
            throw new ApiException(BaseResponse.Code.fail.code, "用户营养目标数量不能超过" + limit);
        }
    }
}
