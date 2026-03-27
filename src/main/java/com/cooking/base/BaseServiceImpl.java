package com.cooking.base;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {

    @Override
    public Map<Long, T> findMapByIds(Set<Long> ids) {
        return super.listByIds(ids).stream().collect(Collectors.toMap(e -> Long.valueOf(ReflectUtil.getFieldValue(e, "id").toString()), e -> e, (e1,e2) -> e1));
    }

    @Override
    public List<T> findListByField(String field, Object value) {
        return super.query().eq(field, value).list();
    }

    @Override
    public List<T> findListByField(String field, List<Object> values) {
        return super.query().in(field, values).list();
    }

    @Override
    public Map<Long, T> findMapByField(String field, Object value) {
        return super.query().eq(field, value).list().stream().collect(Collectors.toMap(e -> Long.valueOf(ReflectUtil.getFieldValue(e, "id").toString()), e -> e, (e1,e2) -> e1));
    }

    @Override
    public Map<Long, T> findMapByField(String field, List<Object> values) {
        return super.query().in(field, values).list().stream().collect(Collectors.toMap(e -> Long.valueOf(ReflectUtil.getFieldValue(e, "id").toString()), e -> e, (e1,e2) -> e1));
    }
}
