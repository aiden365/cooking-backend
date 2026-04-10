package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.SystemParamsEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * System params mapper
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface SystemParamsMapper extends BaseMapper<SystemParamsEntity> {

    IPage<SystemParamsEntity> findPage(IPage<SystemParamsEntity> page, @Param("params") Map<String, Object> params);
}
