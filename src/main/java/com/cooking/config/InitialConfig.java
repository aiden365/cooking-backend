package com.cooking.config;

import cn.hutool.crypto.digest.MD5;
import com.cooking.core.entity.SystemParamsEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.SystemParamsService;
import com.cooking.core.service.UserService;
import com.cooking.enums.SystemParamEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InitialConfig implements ApplicationRunner {

    @Autowired
    private UserService userService;
    @Autowired
    private SystemParamsService systemParamsService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        UserEntity userEntity = Optional.ofNullable(userService.getById(UserEntity.super_admin_id)).orElseGet(UserEntity::new);
        //初始化超级管理员用户
        if(userEntity.getId() == null){
            userEntity.setId(UserEntity.super_admin_id);
            userEntity.setUserCode("admin");
            userEntity.setUserName("超级管理员");
            userEntity.setUserPass(MD5.create().digestHex("admin"));
            userEntity.setStatus(1);
            userEntity.setGender(1);
            userEntity.setAge(18);
            userEntity.setStature(175);
            userEntity.setWeight(60);
            userEntity.setCreateUser(UserEntity.super_admin_id);
            userEntity.setUpdateUser(UserEntity.super_admin_id);
            userEntity.setDeleted(0);
            userEntity.setCreateTime(new Date());
            userEntity.setUpdateTime(new Date());
            userEntity.setType(2);
            userService.save(userEntity);
        }

        //初始化系统参数
        List<SystemParamEnum> systemParamEnums = Arrays.stream(SystemParamEnum.values()).toList();
        List<SystemParamsEntity> paramsEntityList = systemParamsService.list();
        List<SystemParamsEntity> newParamsEntityList = new ArrayList<>();
        for (SystemParamEnum paramEnum : systemParamEnums) {
            SystemParamsEntity paramsEntity = paramsEntityList.stream().filter(paramsEntity1 -> paramsEntity1.getParamName().equals(paramEnum.name())).findFirst().orElse(null);
            if(paramsEntity == null){
                paramsEntity = new SystemParamsEntity();
                paramsEntity.setParamName(paramEnum.name());
                paramsEntity.setParamValue(paramEnum.getValue());
                newParamsEntityList.add(paramsEntity);
            }
        }
        if(!newParamsEntityList.isEmpty()){
            systemParamsService.saveOrUpdateBatch(newParamsEntityList);
        }

    }
}
