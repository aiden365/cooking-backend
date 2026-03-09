package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.UserLabelRelEntity;
import com.cooking.core.mapper.UserLabelRelMapper;
import com.cooking.core.service.UserLabelRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User label rel service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class UserLabelRelServiceImpl extends BaseServiceImpl<UserLabelRelMapper, UserLabelRelEntity> implements UserLabelRelService {

    @Autowired
    private UserLabelRelMapper userLabelRelMapper;

    @Override
    public List<UserLabelRelEntity> findList(Map<String, Object> params) {
        return userLabelRelMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<UserLabelRelEntity> findPage(IPage<UserLabelRelEntity> page, Map<String, Object> params) {
        return userLabelRelMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
