package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.RepositoryEntity;
import com.cooking.core.mapper.RepositoryMapper;
import com.cooking.core.service.RepositoryService;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
        removeByIds(ids);
    }
}
