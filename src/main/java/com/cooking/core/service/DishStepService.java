package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.DishStepEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish step service
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface DishStepService extends BaseService<DishStepEntity> {

    List<DishStepEntity> findList(Map<String, Object> params);

    IPage<DishStepEntity> findPage(IPage<DishStepEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
