package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.UserNutritionRelEntity;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User nutrition service
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface UserNutritionRelService extends BaseService<UserNutritionRelEntity> {

    IPage<UserNutritionRelEntity> findPage(IPage<UserNutritionRelEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
