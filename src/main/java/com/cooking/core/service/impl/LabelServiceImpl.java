package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.LabelEntity;
import com.cooking.core.mapper.LabelMapper;
import com.cooking.core.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Label service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class LabelServiceImpl extends BaseServiceImpl<LabelMapper, LabelEntity> implements LabelService {

    @Autowired
    private LabelMapper labelMapper;


    @Override
    public IPage<LabelEntity> findPage(IPage<LabelEntity> page, Map<String, Object> params) {
        return labelMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
