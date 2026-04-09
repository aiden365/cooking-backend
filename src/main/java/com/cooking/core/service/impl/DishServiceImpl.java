package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.DishFlavorEntity;
import com.cooking.core.entity.DishMaterialEntity;
import com.cooking.core.entity.DishStepEntity;
import com.cooking.core.mapper.DishMapper;
import com.cooking.core.service.DishFlavorService;
import com.cooking.core.service.DishMaterialService;
import com.cooking.core.service.DishService;
import com.cooking.core.service.DishStepService;
import com.cooking.dto.AIRecipeDTO;
import com.cooking.dto.DishSaveDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish service impl
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Service
public class DishServiceImpl extends BaseServiceImpl<DishMapper, DishEntity> implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishMaterialService dishMaterialService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private DishStepService dishStepService;

    @Override
    public List<DishEntity> findList(Map<String, Object> params) {
        return dishMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<DishEntity> findPage(IPage<DishEntity> page, Map<String, Object> params) {
        return dishMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DishEntity saveDish(DishSaveDTO dishSaveDTO) {
        DishEntity dishEntity;
        boolean isCreate = dishSaveDTO.getId() == null;
        if (isCreate) {
            dishEntity = new DishEntity();
            dishEntity.setSourceType(1);
            dishEntity.setViewCount(0L);
            dishEntity.setActiveVal(0);
            dishEntity.setPopularVal(0);
        } else {
            dishEntity = super.getById(dishSaveDTO.getId());
            if (dishEntity == null) {
                throw new IllegalArgumentException("菜谱不存在");
            }
        }

        dishEntity.setName(dishSaveDTO.getName());
        dishEntity.setTakeTimes(dishSaveDTO.getTakeTimes());
        dishEntity.setCheckStatus(dishSaveDTO.getCheckStatus());
        dishEntity.setTips(dishSaveDTO.getTips());
        dishEntity.setImgPath(dishSaveDTO.getImgPath());
        super.saveOrUpdate(dishEntity);

        List<DishFlavorEntity> existingFlavors = dishFlavorService.lambdaQuery()
                .eq(DishFlavorEntity::getDishId, dishEntity.getId())
                .list();
        Set<Long> retainFlavorIds = new HashSet<>();
        if (dishSaveDTO.getFlavors() != null) {
            for (DishSaveDTO.FlavorItem flavorItem : dishSaveDTO.getFlavors()) {
                if (flavorItem == null || !StringUtils.hasText(flavorItem.getFlavorName())) {
                    continue;
                }
                DishFlavorEntity entity = flavorItem.getId() == null ? new DishFlavorEntity() : dishFlavorService.getById(flavorItem.getId());
                if (entity == null) {
                    entity = new DishFlavorEntity();
                }
                if (entity.getId() != null && !dishEntity.getId().equals(entity.getDishId())) {
                    throw new IllegalArgumentException("调料数据不属于当前菜谱");
                }
                entity.setDishId(dishEntity.getId());
                entity.setFlavorName(flavorItem.getFlavorName());
                entity.setDosage(flavorItem.getDosage());
                dishFlavorService.saveOrUpdate(entity);
                retainFlavorIds.add(entity.getId());
            }
        }
        deleteRemovedFlavors(existingFlavors, retainFlavorIds);

        List<DishMaterialEntity> existingMaterials = dishMaterialService.lambdaQuery()
                .eq(DishMaterialEntity::getDishId, dishEntity.getId())
                .list();
        Set<Long> retainMaterialIds = new HashSet<>();
        if (dishSaveDTO.getMaterials() != null) {
            for (DishSaveDTO.MaterialItem materialItem : dishSaveDTO.getMaterials()) {
                if (materialItem == null || !StringUtils.hasText(materialItem.getMaterialName())) {
                    continue;
                }
                DishMaterialEntity entity = materialItem.getId() == null ? new DishMaterialEntity() : dishMaterialService.getById(materialItem.getId());
                if (entity == null) {
                    entity = new DishMaterialEntity();
                }
                if (entity.getId() != null && !dishEntity.getId().equals(entity.getDishId())) {
                    throw new IllegalArgumentException("食材数据不属于当前菜谱");
                }
                entity.setDishId(dishEntity.getId());
                entity.setMaterialName(materialItem.getMaterialName());
                entity.setDosage(materialItem.getDosage());
                entity.setDeal(materialItem.getDeal());
                dishMaterialService.saveOrUpdate(entity);
                retainMaterialIds.add(entity.getId());
            }
        }
        deleteRemovedMaterials(existingMaterials, retainMaterialIds);

        List<DishStepEntity> existingSteps = dishStepService.lambdaQuery()
                .eq(DishStepEntity::getDishId, dishEntity.getId())
                .list();
        Set<Long> retainStepIds = new HashSet<>();
        if (dishSaveDTO.getSteps() != null) {
            for (DishSaveDTO.StepItem stepItem : dishSaveDTO.getSteps()) {
                if (stepItem == null || !StringUtils.hasText(stepItem.getStepDescribe())) {
                    continue;
                }
                DishStepEntity entity = stepItem.getId() == null ? new DishStepEntity() : dishStepService.getById(stepItem.getId());
                if (entity == null) {
                    entity = new DishStepEntity();
                }
                if (entity.getId() != null && !dishEntity.getId().equals(entity.getDishId())) {
                    throw new IllegalArgumentException("步骤数据不属于当前菜谱");
                }
                entity.setDishId(dishEntity.getId());
                entity.setSort(stepItem.getSort());
                entity.setStepDescribe(stepItem.getStepDescribe());
                entity.setStepImage(stepItem.getStepImage());
                dishStepService.saveOrUpdate(entity);
                retainStepIds.add(entity.getId());
            }
        }
        deleteRemovedSteps(existingSteps, retainStepIds);

        return dishEntity;
    }

    @Override
    public DishEntity saveAigcRecipe(AIRecipeDTO aiRecipeDTO) {
        DishEntity existed = super.lambdaQuery().eq(DishEntity::getName, aiRecipeDTO.getDishName()).one();
        if (existed != null) {
            return existed;
        }

        DishEntity dishEntity = new DishEntity();
        dishEntity.setName(aiRecipeDTO.getDishName());
        dishEntity.setTakeTimes(aiRecipeDTO.getTakeTimes());
        dishEntity.setTips(aiRecipeDTO.getTips());
        dishEntity.setSourceType(2);
        dishEntity.setCheckStatus(1);
        dishEntity.setImgPath("");
        dishEntity.setViewCount(0L);
        dishEntity.setActiveVal(0);
        dishEntity.setPopularVal(0);
        super.save(dishEntity);

        List<DishMaterialEntity> materials = new ArrayList<>();
        if (aiRecipeDTO.getMaterials() != null) {
            for (AIRecipeDTO.Materials material : aiRecipeDTO.getMaterials()) {
                if (material == null || !StringUtils.hasText(material.getName())) {
                    continue;
                }
                DishMaterialEntity entity = new DishMaterialEntity();
                entity.setDishId(dishEntity.getId());
                entity.setMaterialName(material.getName());
                entity.setDosage(material.getDosage());
                entity.setDeal(material.getDeal());
                materials.add(entity);
            }
        }
        if (!materials.isEmpty()) {
            dishMaterialService.saveBatch(materials);
        }

        List<DishFlavorEntity> flavors = new ArrayList<>();
        if (aiRecipeDTO.getFlavors() != null) {
            for (AIRecipeDTO.Flavors flavor : aiRecipeDTO.getFlavors()) {
                if (flavor == null || !StringUtils.hasText(flavor.getName())) {
                    continue;
                }
                DishFlavorEntity entity = new DishFlavorEntity();
                entity.setDishId(dishEntity.getId());
                entity.setFlavorName(flavor.getName());
                entity.setDosage(flavor.getDosage());
                flavors.add(entity);
            }
        }
        if (!flavors.isEmpty()) {
            dishFlavorService.saveBatch(flavors);
        }

        List<DishStepEntity> steps = new ArrayList<>();
        if (aiRecipeDTO.getSteps() != null) {
            for (int i = 0; i < aiRecipeDTO.getSteps().size(); i++) {
                AIRecipeDTO.Steps step = aiRecipeDTO.getSteps().get(i);
                if (step == null || !StringUtils.hasText(step.getInstruction())) {
                    continue;
                }
                DishStepEntity entity = new DishStepEntity();
                entity.setDishId(dishEntity.getId());
                entity.setStepDescribe(step.getInstruction());
                entity.setSort(step.getStepNumber() == null ? (i + 1) : step.getStepNumber());
                entity.setStepImage(null);
                steps.add(entity);
            }
        }
        if (!steps.isEmpty()) {
            dishStepService.saveBatch(steps);
        }

        return dishEntity;
    }

    private String buildStepImages(String stepImage) {
        if (!StringUtils.hasText(stepImage)) {
            return "[]";
        }
        return "[\"" + stepImage.replace("\"", "\\\"") + "\"]";
    }

    private void deleteRemovedFlavors(List<DishFlavorEntity> existingFlavors, Set<Long> retainFlavorIds) {
        List<Long> removeIds = existingFlavors.stream()
                .map(DishFlavorEntity::getId)
                .filter(id -> !retainFlavorIds.contains(id))
                .toList();
        if (!removeIds.isEmpty()) {
            dishFlavorService.removeByIds(removeIds);
        }
    }

    private void deleteRemovedMaterials(List<DishMaterialEntity> existingMaterials, Set<Long> retainMaterialIds) {
        List<Long> removeIds = existingMaterials.stream()
                .map(DishMaterialEntity::getId)
                .filter(id -> !retainMaterialIds.contains(id))
                .toList();
        if (!removeIds.isEmpty()) {
            dishMaterialService.removeByIds(removeIds);
        }
    }

    private void deleteRemovedSteps(List<DishStepEntity> existingSteps, Set<Long> retainStepIds) {
        List<Long> removeIds = existingSteps.stream()
                .map(DishStepEntity::getId)
                .filter(id -> !retainStepIds.contains(id))
                .toList();
        if (!removeIds.isEmpty()) {
            dishStepService.removeByIds(removeIds);
        }
    }
}
