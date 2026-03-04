package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.mapper.UserMapper;
import com.cooking.core.service.UserService;
import com.cooking.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private UserMapper userMapper;



    @Override
    public List<UserEntity> findList(Map<String, Object> params) {
        return userMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<UserEntity> findPage(IPage<UserEntity> page, Map<String, Object> params) {
        return userMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
