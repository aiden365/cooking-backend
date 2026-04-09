package com.cooking.core.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.core.entity.LabelEntity;
import com.cooking.core.entity.UserIndividualDishEntity;
import com.cooking.base.BaseService;

import java.util.Map;

/**
 * <p>
 * 用户个性化菜谱 服务类
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
public interface UserIndividualDishService extends BaseService<UserIndividualDishEntity> {


    public IPage<UserIndividualDishEntity> findPage(IPage<UserIndividualDishEntity> page, Map<String, Object> params);
}
