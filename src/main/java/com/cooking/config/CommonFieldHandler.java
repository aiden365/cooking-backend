package com.cooking.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cooking.base.BaseEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.utils.SystemContextHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class CommonFieldHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = Optional.ofNullable(SystemContextHelper.getCurrentUser()).map(BaseEntity::getId).orElse(null);
        if(userId == null){
            userId = UserEntity.super_admin_id;
            log.warn("当前用户不存在，使用超级管理员用户作为添加公共字段信息");
        }
        Long finalUserId = userId;
        this.strictInsertFill(metaObject, "createTime", Date::new, Date.class);
        this.strictInsertFill(metaObject, "updateTime", Date::new, Date.class);
        this.strictInsertFill(metaObject, "createUser", () -> finalUserId, Long.class);
        this.strictUpdateFill(metaObject, "updateUser", () -> finalUserId, Long.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = Optional.ofNullable(SystemContextHelper.getCurrentUser()).map(BaseEntity::getId).orElse(null);
        if(userId == null){
            userId = UserEntity.super_admin_id;
            log.warn("当前用户不存在，使用超级管理员用户作为更新公共字段信息");
        }
        Long finalUserId = userId;
        this.strictUpdateFill(metaObject, "updateTime", Date::new, Date.class);
        this.strictUpdateFill(metaObject, "updateUser", () -> finalUserId, Long.class);
    }

}
