package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.UserShareEntity;
import com.cooking.core.mapper.UserShareMapper;
import com.cooking.core.service.UserShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User share service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class UserShareServiceImpl extends BaseServiceImpl<UserShareMapper, UserShareEntity> implements UserShareService {

    @Autowired
    private UserShareMapper userShareMapper;

    @Override
    public IPage<UserShareEntity> findPage(IPage<UserShareEntity> page, Map<String, Object> params) {
        return userShareMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
