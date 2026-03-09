package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.mapper.DishMapper;
import com.cooking.core.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class DishServiceImpl extends BaseServiceImpl<DishMapper, DishEntity> implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Override
    public List<DishEntity> findList(Map<String, Object> params) {
        return dishMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<DishEntity> findPage(IPage<DishEntity> page, Map<String, Object> params) {
        return dishMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
