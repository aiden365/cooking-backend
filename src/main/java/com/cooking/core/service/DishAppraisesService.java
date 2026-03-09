package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.DishAppraisesEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish appraises service
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface DishAppraisesService extends BaseService<DishAppraisesEntity> {

    IPage<DishAppraisesEntity> findPage(IPage<DishAppraisesEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
