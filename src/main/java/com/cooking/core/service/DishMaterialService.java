package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.DishMaterialEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish material service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface DishMaterialService extends BaseService<DishMaterialEntity> {

    IPage<DishMaterialEntity> findPage(IPage<DishMaterialEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
