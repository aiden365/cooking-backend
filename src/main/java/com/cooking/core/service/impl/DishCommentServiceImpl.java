package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishCommentEntity;
import com.cooking.core.entity.UserShareCommentEntity;
import com.cooking.core.mapper.DishCommentMapper;
import com.cooking.core.service.DishCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish comment service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class DishCommentServiceImpl extends BaseServiceImpl<DishCommentMapper, DishCommentEntity> implements DishCommentService {

    @Autowired
    private DishCommentMapper dishCommentMapper;

    @Override
    public List<DishCommentEntity> findList(Map<String, Object> params) {
        return dishCommentMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<DishCommentEntity> findPage(IPage<DishCommentEntity> page, Map<String, Object> params) {
        return dishCommentMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }
    @Override
    public void incrementStartCount(Long id) {
        UpdateWrapper<DishCommentEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id).setSql("start_count = start_count + 1");
        update(updateWrapper);
    }
}
