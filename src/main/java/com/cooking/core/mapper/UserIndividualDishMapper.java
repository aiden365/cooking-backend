package com.cooking.core.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.LabelEntity;
import com.cooking.core.entity.UserIndividualDishEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * <p>
 * 用户个性化菜谱 Mapper 接口
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
public interface UserIndividualDishMapper extends BaseMapper<UserIndividualDishEntity> {

    IPage<UserIndividualDishEntity> findPage(IPage<UserIndividualDishEntity> page, @Param("params") Map<String, Object> params);
}
