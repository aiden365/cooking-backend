package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.core.entity.NutritionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Map;

/**
 * <p>
 * 营养表 Mapper 接口
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
public interface NutritionMapper extends BaseMapper<NutritionEntity> {

    IPage<NutritionEntity> findPage(Page<NutritionEntity> page, Map<String, Object> params);
}
