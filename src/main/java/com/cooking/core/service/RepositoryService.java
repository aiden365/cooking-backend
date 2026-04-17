package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.RepositoryEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Repository service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface RepositoryService extends BaseService<RepositoryEntity> {

    List<RepositoryEntity> findList(Map<String, Object> params);

    IPage<RepositoryEntity> findPage(IPage<RepositoryEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);

    void saveToVectorStore(RepositoryEntity repositoryEntity);

    void deleteFromVectorStore(Collection<Long> repositoryIds);

    Map<String, Object> rebuildAllVectorStore();
}
