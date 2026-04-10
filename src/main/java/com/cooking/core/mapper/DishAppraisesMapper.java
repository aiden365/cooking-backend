package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.DishAppraisesEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * Dish appraises mapper
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface DishAppraisesMapper extends BaseMapper<DishAppraisesEntity> {

    IPage<DishAppraisesEntity> findPage(IPage<DishAppraisesEntity> page, @Param("params") Map<String, Object> params);
}
