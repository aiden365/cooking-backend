package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishLableRelEntity;
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
public class DishLableRelServiceImpl extends BaseServiceImpl<DishLableRelMapper, DishLableRelEntity> implements DishLableRelService {

    @Autowired
    private DishLableRelMapper dishLableRelMapper;

    @Override
    public List<DishLableRelEntity> findList(Map<String, Object> params) {
        return dishLableRelMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<DishLableRelEntity> findPage(IPage<DishLableRelEntity> page, Map<String, Object> params) {
        return dishLableRelMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
