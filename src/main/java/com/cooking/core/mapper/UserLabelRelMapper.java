package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.UserLabelRelEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * User label rel mapper
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface UserLabelRelMapper extends BaseMapper<UserLabelRelEntity> {

    IPage<UserLabelRelEntity> findPage(IPage<UserLabelRelEntity> page, @Param("params") Map<String, Object> params);
}
