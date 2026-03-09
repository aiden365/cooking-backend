package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.DishStepEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * Dish step mapper
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface DishStepMapper extends BaseMapper<DishStepEntity> {

    IPage<DishStepEntity> findPage(IPage<DishStepEntity> page, @Param("params") Map<String, Object> params);
}
