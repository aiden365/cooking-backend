package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.UserDietRecordEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User diet record service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface UserDietRecordService extends BaseService<UserDietRecordEntity> {

    IPage<UserDietRecordEntity> findPage(IPage<UserDietRecordEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
