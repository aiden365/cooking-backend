package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.DishLabelRelEntity;

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
public interface DishLableRelService extends BaseService<DishLabelRelEntity> {

    List<DishLabelRelEntity> findList(Map<String, Object> params);

    IPage<DishLabelRelEntity> findPage(IPage<DishLabelRelEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);

    void saveDishLabels(Long dishId, List<Long> labelIds);
}
