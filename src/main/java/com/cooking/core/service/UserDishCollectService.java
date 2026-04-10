package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.UserDishCollectEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User dish collect service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface UserDishCollectService extends BaseService<UserDishCollectEntity> {

    IPage<UserDishCollectEntity> findPage(IPage<UserDishCollectEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
