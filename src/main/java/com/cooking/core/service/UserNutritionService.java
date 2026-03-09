package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.UserNutritionEntity;

import java.util.List;
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
public interface UserNutritionService extends BaseService<UserNutritionEntity> {

    IPage<UserNutritionEntity> findPage(IPage<UserNutritionEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
