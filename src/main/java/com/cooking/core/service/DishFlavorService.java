package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.DishFlavorEntity;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish flavor service
 * </p>
 */
public interface DishFlavorService extends BaseService<DishFlavorEntity> {

    IPage<DishFlavorEntity> findPage(IPage<DishFlavorEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
