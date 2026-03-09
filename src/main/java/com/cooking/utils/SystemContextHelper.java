package com.cooking.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.cooking.core.entity.UserEntity;

public class SystemContextHelper {

    private static final TransmittableThreadLocal<UserEntity> user = new TransmittableThreadLocal<>();

    /**
     * 设置当前用户
     */
    public static void setCurrentUser(UserEntity userEntity) {
        user.set(userEntity);
    }

    /**
     * 兼容旧调用
     */
    public static void setUser(UserEntity userEntity) {
        setCurrentUser(userEntity);
    }

    /**
     * 获取当前用户
     */
    public static UserEntity getCurrentUser() {
        return user.get();
    }
}
