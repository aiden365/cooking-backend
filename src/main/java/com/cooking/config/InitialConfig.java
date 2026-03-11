package com.cooking.config;

import cn.hutool.crypto.digest.MD5;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Component
public class InitialConfig implements ApplicationRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        UserEntity userEntity = Optional.ofNullable(userService.getById(UserEntity.super_admin_id)).orElseGet(UserEntity::new);
        //初始化超级管理员用户
        if(userEntity.getId() == null){
            userEntity.setUserCode("admin");
            userEntity.setUserName("超级管理员");
            userEntity.setUserPass(MD5.create().digestHex("admin"));
            userEntity.setCreateUser(UserEntity.super_admin_id);
            userEntity.setUpdateUser(UserEntity.super_admin_id);
            userEntity.setDeleted(0);
            userEntity.setCreateTime(new Date());
            userEntity.setUpdateTime(new Date());
            userEntity.setType(3);
            userService.save(userEntity);
        }
    }
}
