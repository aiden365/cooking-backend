package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.UserShareEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User share service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface UserShareService extends BaseService<UserShareEntity> {

    IPage<UserShareEntity> findPage(IPage<UserShareEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
