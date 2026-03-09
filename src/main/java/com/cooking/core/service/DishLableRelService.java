package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.DishLableRelEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish lable rel service
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface DishLableRelService extends BaseService<DishLableRelEntity> {

    List<DishLableRelEntity> findList(Map<String, Object> params);

    IPage<DishLableRelEntity> findPage(IPage<DishLableRelEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
