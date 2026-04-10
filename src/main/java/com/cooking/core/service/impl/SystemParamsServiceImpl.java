package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.SystemParamsEntity;
import com.cooking.core.mapper.SystemParamsMapper;
import com.cooking.core.service.SystemParamsService;
import com.cooking.enums.SystemParamEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * System params service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class SystemParamsServiceImpl extends BaseServiceImpl<SystemParamsMapper, SystemParamsEntity> implements SystemParamsService {

    @Autowired
    private SystemParamsMapper systemParamsMapper;

    @Override
    public List<SystemParamsEntity> findList(Map<String, Object> params) {
        return systemParamsMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<SystemParamsEntity> findPage(IPage<SystemParamsEntity> page, Map<String, Object> params) {
        return systemParamsMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }

    @Override
    public SystemParamsEntity findByName(SystemParamEnum paramEnum) {
        SystemParamsEntity systemParamsEntity = super.lambdaQuery().eq(SystemParamsEntity::getParamName, paramEnum.value).list().stream().findAny().orElse(null);
        if(systemParamsEntity == null){
            systemParamsEntity = new SystemParamsEntity();
            systemParamsEntity.setParamName(paramEnum.name());
            systemParamsEntity.setParamValue(paramEnum.getValue());
        }
        return systemParamsEntity;
    }
}
