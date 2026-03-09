package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.RepositoryEntity;
import com.cooking.core.mapper.RepositoryMapper;
import com.cooking.core.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Repository service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class RepositoryServiceImpl extends BaseServiceImpl<RepositoryMapper, RepositoryEntity> implements RepositoryService {

    @Autowired
    private RepositoryMapper repositoryMapper;

    @Override
    public List<RepositoryEntity> findList(Map<String, Object> params) {
        return repositoryMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<RepositoryEntity> findPage(IPage<RepositoryEntity> page, Map<String, Object> params) {
        return repositoryMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
