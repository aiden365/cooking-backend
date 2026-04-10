package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.UserLabelRelEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User label rel service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface UserLabelRelService extends BaseService<UserLabelRelEntity> {

    List<UserLabelRelEntity> findList(Map<String, Object> params);

    IPage<UserLabelRelEntity> findPage(IPage<UserLabelRelEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);

    void saveUserLabels(Long userId, List<Long> labelIds);
}
