package com.cooking.core.service.impl;

import com.cooking.core.entity.UserEntity;
import com.cooking.core.mapper.UserMapper;
import com.cooking.core.service.UserService;
import com.cooking.base.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author aiden
 * @since 2026-02-10
 */
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, UserEntity> implements UserService {

}
