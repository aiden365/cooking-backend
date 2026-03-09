package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.UserShareCommentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * User share comment mapper
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface UserShareCommentMapper extends BaseMapper<UserShareCommentEntity> {

    IPage<UserShareCommentEntity> findPage(IPage<UserShareCommentEntity> page, @Param("params") Map<String, Object> params);
}
