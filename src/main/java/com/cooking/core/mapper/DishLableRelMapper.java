package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.DishLabelRelEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * Dish lable rel mapper
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface DishLableRelMapper extends BaseMapper<DishLabelRelEntity> {

    IPage<DishLabelRelEntity> findPage(IPage<DishLabelRelEntity> page, @Param("params") Map<String, Object> params);
}
