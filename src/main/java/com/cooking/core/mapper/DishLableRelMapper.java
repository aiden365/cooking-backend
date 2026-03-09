package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.DishLableRelEntity;
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
public interface DishLableRelMapper extends BaseMapper<DishLableRelEntity> {

    IPage<DishLableRelEntity> findPage(IPage<DishLableRelEntity> page, @Param("params") Map<String, Object> params);
}
