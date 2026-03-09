package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.DishCommentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * Dish comment mapper
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface DishCommentMapper extends BaseMapper<DishCommentEntity> {

    IPage<DishCommentEntity> findPage(IPage<DishCommentEntity> page, @Param("params") Map<String, Object> params);
}
