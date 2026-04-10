package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.core.entity.NutritionEntity;
import com.cooking.core.mapper.NutritionMapper;
import com.cooking.core.service.NutritionService;
import com.cooking.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * <p>
 * 营养表 服务实现类
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
@Service
public class NutritionServiceImpl extends BaseServiceImpl<NutritionMapper, NutritionEntity> implements NutritionService {

    @Autowired
    private NutritionMapper nutritionMapper;

    @Override
    public IPage<NutritionEntity> findPage(Page<NutritionEntity> page, Map<String, Object> params) {
        return nutritionMapper.findPage(page, params);
    }
}
