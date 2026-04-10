package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.UserEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface UserMapper extends BaseMapper<UserEntity> {

    IPage<UserEntity> findPage(IPage<UserEntity> page, @Param("params") Map<String, Object> params);
}
