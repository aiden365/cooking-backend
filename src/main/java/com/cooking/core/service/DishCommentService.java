package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.DishCommentEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish comment service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface DishCommentService extends BaseService<DishCommentEntity> {

    List<DishCommentEntity> findList(Map<String, Object> params);

    IPage<DishCommentEntity> findPage(IPage<DishCommentEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);

    void incrementStartCount(Long id);

}
