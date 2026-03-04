package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.UserEntity;
import com.cooking.base.BaseService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface UserService extends BaseService<UserEntity> {

    List<UserEntity> findList(Map<String, Object> params);
    IPage<UserEntity> findPage(IPage<UserEntity> page, Map<String, Object> params);
    void deleteByIds(Set<String> ids);
}
