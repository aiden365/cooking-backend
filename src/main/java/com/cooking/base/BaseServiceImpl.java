package com.cooking.base;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {

    @Override
    public Map<Long, T> findMapByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return super.listByIds(ids).stream().collect(Collectors.toMap(e -> Long.valueOf(ReflectUtil.getFieldValue(e, "id").toString()), e -> e, (e1,e2) -> e1));
    }

    @Override
    public List<T> findListByField(String field, Object value) {
        return super.query().eq(StrUtil.toUnderlineCase(field), value).list();
    }


    @Override
    public Map<Long, T> findMapByField(String field, Object value) {
        if (value == null) {
            return null;
        }

        QueryChainWrapper<T> query = super.query();
        String columnName = StrUtil.toUnderlineCase(field);

        if (value instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                return Collections.EMPTY_MAP;
            }
            query.in(columnName, collection);
        } else if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            if (array.length == 0) {
                return Collections.EMPTY_MAP;
            }
            query.in(columnName, array);
        } else {
            query.eq(columnName, value);
        }

        List<T> list = query.list();
        if (list == null || list.isEmpty()) {
            return Collections.EMPTY_MAP;
        }

        return list.stream().collect(Collectors.toMap(e -> Long.valueOf(ReflectUtil.getFieldValue(e, "id").toString()),e -> e,(e1, e2) -> e1));
    }

}
