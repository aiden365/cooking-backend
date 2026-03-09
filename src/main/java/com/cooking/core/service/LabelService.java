package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.LabelEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Label service
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
public interface LabelService extends BaseService<LabelEntity> {


    IPage<LabelEntity> findPage(IPage<LabelEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);
}
