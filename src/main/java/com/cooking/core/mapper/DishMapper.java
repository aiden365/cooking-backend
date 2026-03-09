package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.DishEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * Dish mapper
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface DishMapper extends BaseMapper<DishEntity> {

    IPage<DishEntity> findPage(IPage<DishEntity> page, @Param("params") Map<String, Object> params);
}
