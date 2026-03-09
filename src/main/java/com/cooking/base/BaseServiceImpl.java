package com.cooking.base;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {

    @Override
    public Map<Long, T> findMapByIds(Set<Long> ids) {
        return super.listByIds(ids).stream().collect(Collectors.toMap(e -> Long.valueOf(ReflectUtil.getFieldValue(e, "id").toString()), e -> e, (e1,e2) -> e1));
    }
}
