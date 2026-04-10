package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.UserDishCollectEntity;
import com.cooking.core.mapper.UserDishCollectMapper;
import com.cooking.core.service.UserDishCollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User dish collect service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class UserDishCollectServiceImpl extends BaseServiceImpl<UserDishCollectMapper, UserDishCollectEntity> implements UserDishCollectService {

    @Autowired
    private UserDishCollectMapper userDishCollectMapper;

    @Override
    public IPage<UserDishCollectEntity> findPage(IPage<UserDishCollectEntity> page, Map<String, Object> params) {
        return userDishCollectMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
