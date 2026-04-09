package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.UserNutritionRelEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * User nutrition mapper
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface UserNutritionRelMapper extends BaseMapper<UserNutritionRelEntity> {

    IPage<UserNutritionRelEntity> findPage(IPage<UserNutritionRelEntity> page, @Param("params") Map<String, Object> params);
}
