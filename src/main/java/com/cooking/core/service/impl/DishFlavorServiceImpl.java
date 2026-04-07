package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishFlavorEntity;
import com.cooking.core.mapper.DishFlavorMapper;
import com.cooking.core.service.DishFlavorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish flavor service impl
 * </p>
 */
@Service
public class DishFlavorServiceImpl extends BaseServiceImpl<DishFlavorMapper, DishFlavorEntity> implements DishFlavorService {

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Override
    public IPage<DishFlavorEntity> findPage(IPage<DishFlavorEntity> page, Map<String, Object> params) {
        return dishFlavorMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
