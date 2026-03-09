package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.UserShareEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * User share mapper
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface UserShareMapper extends BaseMapper<UserShareEntity> {

    IPage<UserShareEntity> findPage(IPage<UserShareEntity> page, @Param("params") Map<String, Object> params);
}
