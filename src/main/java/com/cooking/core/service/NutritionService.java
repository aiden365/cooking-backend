package com.cooking.core.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.core.entity.NutritionEntity;
import com.cooking.base.BaseService;

import java.util.Map;

/**
 * <p>
 * 营养表 服务类
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
public interface NutritionService extends BaseService<NutritionEntity> {

    IPage<NutritionEntity> findPage(Page<NutritionEntity> page, Map<String, Object> params);
}
