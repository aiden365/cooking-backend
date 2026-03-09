package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.UserNutritionEntity;
import com.cooking.core.mapper.UserNutritionMapper;
import com.cooking.core.service.UserNutritionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User nutrition service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class UserNutritionServiceImpl extends BaseServiceImpl<UserNutritionMapper, UserNutritionEntity> implements UserNutritionService {

    @Autowired
    private UserNutritionMapper userNutritionMapper;

    @Override
    public IPage<UserNutritionEntity> findPage(IPage<UserNutritionEntity> page, Map<String, Object> params) {
        return userNutritionMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
