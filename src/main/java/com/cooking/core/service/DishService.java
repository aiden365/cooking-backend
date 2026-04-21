package com.cooking.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cooking.base.BaseService;
import com.cooking.core.entity.DishEntity;
import com.cooking.dto.AIRecipeDTO;
import com.cooking.dto.DishSaveDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Dish service
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
public interface DishService extends BaseService<DishEntity> {

    List<DishEntity> findList(Map<String, Object> params);

    IPage<DishEntity> findPage(IPage<DishEntity> page, Map<String, Object> params);

    void deleteByIds(Set<String> ids);

    DishEntity saveAigcRecipe(AIRecipeDTO aiRecipeDTO);

    DishEntity saveDish(DishSaveDTO dishSaveDTO);

    void saveDishToVectorStore(DishEntity dishEntity);

    Map<String, Object> rebuildAllVectorStore();

    Map<String, Object> diagnoseVectorStore();

    String searchDishImageAndDownload(String dishName);
}
