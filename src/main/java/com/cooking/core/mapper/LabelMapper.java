package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.LabelEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * Label mapper
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface LabelMapper extends BaseMapper<LabelEntity> {

    IPage<LabelEntity> findPage(IPage<LabelEntity> page, @Param("params") Map<String, Object> params);
}
