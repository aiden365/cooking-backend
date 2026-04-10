package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.SystemParamsEntity;
import com.cooking.enums.SystemParamEnum;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * System params service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface SystemParamsService extends BaseService<SystemParamsEntity> {

    List<SystemParamsEntity> findList(Map<String, Object> params);

    IPage<SystemParamsEntity> findPage(IPage<SystemParamsEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);

    SystemParamsEntity findByName(SystemParamEnum paramEnum);
}
