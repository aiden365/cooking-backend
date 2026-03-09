package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.UserNutritionEntity;
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
public interface UserNutritionMapper extends BaseMapper<UserNutritionEntity> {

    IPage<UserNutritionEntity> findPage(IPage<UserNutritionEntity> page, @Param("params") Map<String, Object> params);
}
