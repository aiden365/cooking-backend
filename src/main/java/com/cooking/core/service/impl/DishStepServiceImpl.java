package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishStepEntity;
import com.cooking.core.mapper.DishStepMapper;
import com.cooking.core.service.DishStepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish step service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class DishStepServiceImpl extends BaseServiceImpl<DishStepMapper, DishStepEntity> implements DishStepService {

    @Autowired
    private DishStepMapper dishStepMapper;

    @Override
    public List<DishStepEntity> findList(Map<String, Object> params) {
        return dishStepMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<DishStepEntity> findPage(IPage<DishStepEntity> page, Map<String, Object> params) {
        return dishStepMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
