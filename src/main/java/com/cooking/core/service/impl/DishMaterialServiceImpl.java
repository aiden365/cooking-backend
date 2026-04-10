package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishMaterialEntity;
import com.cooking.core.mapper.DishMaterialMapper;
import com.cooking.core.service.DishMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish material service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class DishMaterialServiceImpl extends BaseServiceImpl<DishMaterialMapper, DishMaterialEntity> implements DishMaterialService {

    @Autowired
    private DishMaterialMapper dishMaterialMapper;


    @Override
    public IPage<DishMaterialEntity> findPage(IPage<DishMaterialEntity> page, Map<String, Object> params) {
        return dishMaterialMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
