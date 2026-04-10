package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.NutritionEntity;
import com.cooking.core.entity.RepositoryEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.NutritionService;
import com.cooking.core.service.UserNutritionRelService;
import com.cooking.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cooking.base.BaseController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * 营养表 前端控制器
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
@RestController
@RequestMapping("nutrition")
public class NutritionApi extends BaseController {

    @Autowired
    private NutritionService nutritionService;
    @Autowired
    private UserNutritionRelService userNutritionRelService;
    @Autowired
    private UserService userService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        int pageNo = params.getIntValue("pageNo");
        int pageSize = params.getIntValue("pageSize");
        IPage<NutritionEntity> entityIPage = nutritionService.findPage(new Page<>(pageNo, pageSize), params);
        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(BaseEntity::getCreateUser).collect(Collectors.toSet()));
        for (NutritionEntity record : entityIPage.getRecords()) {
            record.setCreatorName(Optional.ofNullable(userEntityMap.get(record.getCreateUser())).map(UserEntity::getUserName).orElse(""));
        }
        return ok(entityIPage);
    }

    @PostMapping("detail")
    public BaseResponse detail(@RequestBody JSONObject params) {
        Long id = params.getLong("id");

        if (!NutritionEntity.validId(id)) {
            return fail("id不能为空");
        }
        NutritionEntity entity = nutritionService.getById(id);
        if (entity == null) {
            return fail("营养不存在");
        }

        return ok(entity);
    }

    @PostMapping("save")
    public BaseResponse save(@RequestBody NutritionEntity nutritionEntity) {
        if (!StringUtils.hasText(nutritionEntity.getName())) {
            return fail("营养名称不能为空");
        }
        if (!StringUtils.hasText(nutritionEntity.getDefaultValue())) {
            return fail("默认值不能为空");
        }
        nutritionService.saveOrUpdate(nutritionEntity);
        return ok(nutritionEntity);
    }

    @PostMapping("delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(@RequestBody JSONObject params) {
        List<Long> ids = params.getList("ids", Long.class);
        if (ids == null || ids.isEmpty()) {
            return fail("ids不能为空");
        }
        userNutritionRelService.lambdaUpdate().in(com.cooking.core.entity.UserNutritionRelEntity::getNutritionId, ids).remove();
        nutritionService.removeByIds(ids);
        return ok();
    }
}
