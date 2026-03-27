package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishLabelRelEntity;
import com.cooking.core.mapper.DishLableRelMapper;
import com.cooking.core.service.DishLableRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish lable rel service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class DishLableRelServiceImpl extends BaseServiceImpl<DishLableRelMapper, DishLabelRelEntity> implements DishLableRelService {

    @Autowired
    private DishLableRelMapper dishLableRelMapper;

    @Override
    public List<DishLabelRelEntity> findList(Map<String, Object> params) {
        return dishLableRelMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<DishLabelRelEntity> findPage(IPage<DishLabelRelEntity> page, Map<String, Object> params) {
        return dishLableRelMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
