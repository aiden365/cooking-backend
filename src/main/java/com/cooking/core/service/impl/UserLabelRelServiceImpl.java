package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.UserLabelRelEntity;
import com.cooking.core.mapper.UserLabelRelMapper;
import com.cooking.core.service.UserLabelRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.util.CollectionUtils;

import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * User label rel service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class UserLabelRelServiceImpl extends BaseServiceImpl<UserLabelRelMapper, UserLabelRelEntity> implements UserLabelRelService {

    @Autowired
    private UserLabelRelMapper userLabelRelMapper;

    @Override
    public List<UserLabelRelEntity> findList(Map<String, Object> params) {
        return userLabelRelMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<UserLabelRelEntity> findPage(IPage<UserLabelRelEntity> page, Map<String, Object> params) {
        return userLabelRelMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {
        removeByIds(ids);
    }

    @Override
    @Transactional
    public void saveUserLabels(Long userId, List<Long> labelIds) {
        List<UserLabelRelEntity> existingRels = list(new LambdaQueryWrapper<UserLabelRelEntity>().eq(UserLabelRelEntity::getUserId, userId));
        Set<Long> existingLabelIds = existingRels.stream().map(UserLabelRelEntity::getLabelId).collect(Collectors.toSet());

        Set<Long> newLabelIds = (labelIds == null) ? Collections.emptySet() : new HashSet<>(labelIds);

        Set<Long> toDelete = new HashSet<>(existingLabelIds);
        toDelete.removeAll(newLabelIds);

        Set<Long> toAdd = new HashSet<>(newLabelIds);
        toAdd.removeAll(existingLabelIds);

        if (!toDelete.isEmpty()) {
            remove(new LambdaQueryWrapper<UserLabelRelEntity>().eq(UserLabelRelEntity::getUserId, userId).in(UserLabelRelEntity::getLabelId, toDelete));
        }

        if (!toAdd.isEmpty()) {
            List<UserLabelRelEntity> newUserLabels = toAdd.stream().map(labelId -> UserLabelRelEntity.builder().userId(userId).labelId(labelId).build()).collect(Collectors.toList());
            saveBatch(newUserLabels);
        }
    }
}
