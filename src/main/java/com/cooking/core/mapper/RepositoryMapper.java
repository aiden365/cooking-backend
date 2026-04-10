package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.RepositoryEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * Repository mapper
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface RepositoryMapper extends BaseMapper<RepositoryEntity> {

    IPage<RepositoryEntity> findPage(IPage<RepositoryEntity> page, @Param("params") Map<String, Object> params);
}
