package com.cooking.api;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.NutritionEntity;
import com.cooking.core.entity.SystemParamsEntity;
import com.cooking.core.service.SystemParamsService;
import com.cooking.enums.SystemParamEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * System params controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@RestController
@RequestMapping("system/param")
public class SystemParamsApi extends BaseController {

    @Autowired
    private SystemParamsService systemParamsService;

    @PostMapping("list")
    public BaseResponse list(@RequestBody JSONObject params) {
        String search = params.getString("search");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        Map<String, SystemParamEnum> paramEnumMap = Arrays.stream(SystemParamEnum.values()).collect(Collectors.toMap(SystemParamEnum::name, e -> e));
        List<JSONObject> jsonObjectList = systemParamsService.findList(queryParams).stream().map(e -> {
            SystemParamEnum systemParamEnum = paramEnumMap.get(e.getParamName());
            JSONObject json = new JSONObject();
            json.put("id", e.getId());
            json.put("key", e.getParamName());
            json.put("value", e.getParamValue());
            json.put("name", systemParamEnum.getName());
            json.put("description", systemParamEnum.getDesc());


            return json;
        }).toList();
        return ok(jsonObjectList);
    }


    @PostMapping("detail")
    public BaseResponse detail(@RequestBody JSONObject params) {
        String key = params.getString("key");
        if (StrUtil.isBlank(key)) {
            return fail("id不能为空");
        }
        SystemParamsEntity entity = systemParamsService.lambdaQuery().eq(SystemParamsEntity::getParamName, key).one();
        if (entity == null) {
            return fail("营养不存在");
        }

        SystemParamEnum systemParamEnum = SystemParamEnum.getByName(entity.getParamName());

        if (systemParamEnum == null) {
            return fail("参数不存在");
        }

        JSONObject json = new JSONObject();
        json.put("id", entity.getId());
        json.put("key", entity.getParamName());
        json.put("value", entity.getParamValue());
        json.put("name", systemParamEnum.getName());
        json.put("description", systemParamEnum.getDesc());
        return ok(json);
    }

    @PostMapping("save")
    public BaseResponse save(@RequestBody SystemParamsEntity paramsEntity) {

        if(!BaseEntity.validId(paramsEntity.getId())){
            return fail("id不能为空");
        }
        if (StringUtils.isEmpty(paramsEntity.getParamValue())) {
            return fail("参数值不能为空");
        }

        systemParamsService.saveOrUpdate(paramsEntity);
        return ok(paramsEntity);
    }

}
