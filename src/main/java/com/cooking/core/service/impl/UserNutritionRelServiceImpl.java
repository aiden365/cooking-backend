package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.UserNutritionRelEntity;
import com.cooking.core.mapper.UserNutritionRelMapper;
import com.cooking.core.service.UserNutritionRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User nutrition service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class UserNutritionRelServiceImpl extends BaseServiceImpl<UserNutritionRelMapper, UserNutritionRelEntity> implements UserNutritionRelService {

    @Autowired
    private UserNutritionRelMapper userNutritionRelMapper;

    @Override
    public IPage<UserNutritionRelEntity> findPage(IPage<UserNutritionRelEntity> page, Map<String, Object> params) {
        return userNutritionRelMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
}
