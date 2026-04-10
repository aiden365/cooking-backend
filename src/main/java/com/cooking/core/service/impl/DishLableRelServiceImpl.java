package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishLabelRelEntity;
import com.cooking.core.entity.UserLabelRelEntity;
import com.cooking.core.mapper.DishLableRelMapper;
import com.cooking.core.service.DishLableRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Dish lable rel service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class DishLableRelServiceImpl extends BaseServiceImpl<DishLableRelMapper, DishLabelRelEntity> implements DishLableRelService {

    @Autowired
    private DishLableRelMapper dishLableRelMapper;

    @Override
    public List<DishLabelRelEntity> findList(Map<String, Object> params) {
        return dishLableRelMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<DishLabelRelEntity> findPage(IPage<DishLabelRelEntity> page, Map<String, Object> params) {
        return dishLableRelMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }

    @Override
    public void saveDishLabels(Long dishId, List<Long> labelIds) {
        List<DishLabelRelEntity> existingRels = list(new LambdaQueryWrapper<DishLabelRelEntity>().eq(DishLabelRelEntity::getDishId, dishId));
        Set<Long> existingLabelIds = existingRels.stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet());

        Set<Long> newLabelIds = (labelIds == null) ? Collections.emptySet() : new HashSet<>(labelIds);

        Set<Long> toDelete = new HashSet<>(existingLabelIds);
        toDelete.removeAll(newLabelIds);

        Set<Long> toAdd = new HashSet<>(newLabelIds);
        toAdd.removeAll(existingLabelIds);

        if (!toDelete.isEmpty()) {
            remove(new LambdaQueryWrapper<DishLabelRelEntity>().eq(DishLabelRelEntity::getDishId, dishId).in(DishLabelRelEntity::getLabelId, toDelete));
        }

        if (!toAdd.isEmpty()) {
            List<DishLabelRelEntity> newLabels = toAdd.stream().map(labelId -> DishLabelRelEntity.builder().dishId(dishId).labelId(labelId).build()).collect(Collectors.toList());
            saveBatch(newLabels);
        }
    }
}
