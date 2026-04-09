package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.UserShareCommentEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User share comment service
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface UserShareCommentService extends BaseService<UserShareCommentEntity> {

    IPage<UserShareCommentEntity> findPage(IPage<UserShareCommentEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);

    void incrementStartCount(Long id);
}
