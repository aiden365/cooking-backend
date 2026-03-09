package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.DishMaterialEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * Dish material mapper
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface DishMaterialMapper extends BaseMapper<DishMaterialEntity> {

    IPage<DishMaterialEntity> findPage(IPage<DishMaterialEntity> page, @Param("params") Map<String, Object> params);
}
