package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.UserDietRecordEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * User diet record mapper
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface UserDietRecordMapper extends BaseMapper<UserDietRecordEntity> {

    IPage<UserDietRecordEntity> findPage(IPage<UserDietRecordEntity> page, @Param("params") Map<String, Object> params);
}
