package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.UserDietRecordEntity;
import com.cooking.core.mapper.UserDietRecordMapper;
import com.cooking.core.service.UserDietRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User diet record service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class UserDietRecordServiceImpl extends BaseServiceImpl<UserDietRecordMapper, UserDietRecordEntity> implements UserDietRecordService {

    @Autowired
    private UserDietRecordMapper userDietRecordMapper;

    @Override
    public IPage<UserDietRecordEntity> findPage(IPage<UserDietRecordEntity> page, Map<String, Object> params) {
        return userDietRecordMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
