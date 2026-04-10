package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.UserShareCommentEntity;
import com.cooking.core.mapper.UserShareCommentMapper;
import com.cooking.core.service.UserShareCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User share comment service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class UserShareCommentServiceImpl extends BaseServiceImpl<UserShareCommentMapper, UserShareCommentEntity> implements UserShareCommentService {

    @Autowired
    private UserShareCommentMapper userShareCommentMapper;

    @Override
    public IPage<UserShareCommentEntity> findPage(IPage<UserShareCommentEntity> page, Map<String, Object> params) {
        return userShareCommentMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {
        removeByIds(ids);
    }

    @Override
    public void incrementStartCount(Long id) {
        UpdateWrapper<UserShareCommentEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id).setSql("start_count = start_count + 1");
        update(updateWrapper);
    }
}
