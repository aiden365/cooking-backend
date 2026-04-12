package com.cooking.base;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BaseService<T> extends IService<T> {

    Map<Long, T> findMapByIds(Set<Long> ids);

    List<T> findListByField(String field, Object value);
    Map<Long, T> findMapByField(String field, Object value);
}
