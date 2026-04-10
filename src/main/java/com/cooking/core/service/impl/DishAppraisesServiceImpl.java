package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishAppraisesEntity;
import com.cooking.core.mapper.DishAppraisesMapper;
import com.cooking.core.service.DishAppraisesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish appraises service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class DishAppraisesServiceImpl extends BaseServiceImpl<DishAppraisesMapper, DishAppraisesEntity> implements DishAppraisesService {

    @Autowired
    private DishAppraisesMapper dishAppraisesMapper;


    @Override
    public IPage<DishAppraisesEntity> findPage(IPage<DishAppraisesEntity> page, Map<String, Object> params) {
        return dishAppraisesMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
