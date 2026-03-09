package com.cooking.base;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;
import java.util.Set;

public interface BaseService<T> extends IService<T> {

    Map<Long, T> findMapByIds(Set<Long> ids);
}
