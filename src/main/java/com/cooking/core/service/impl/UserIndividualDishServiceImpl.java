package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.core.entity.LabelEntity;
import com.cooking.core.entity.UserIndividualDishEntity;
import com.cooking.core.mapper.UserIndividualDishMapper;
import com.cooking.core.service.UserIndividualDishService;
import com.cooking.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * <p>
 * 用户个性化菜谱 服务实现类
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
@Service
public class UserIndividualDishServiceImpl extends BaseServiceImpl<UserIndividualDishMapper, UserIndividualDishEntity> implements UserIndividualDishService {

    @Autowired
    private UserIndividualDishMapper userIndividualDishMapper;

    @Override
    public IPage<UserIndividualDishEntity> findPage(IPage<UserIndividualDishEntity> page, Map<String, Object> params) {
        return userIndividualDishMapper.findPage(page, params);
    }
}
