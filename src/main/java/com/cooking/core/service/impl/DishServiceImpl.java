package com.cooking.core.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
import com.cooking.enums.PathEnum;
import com.cooking.exceptions.OtherException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Dish service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Slf4j
@Service
public class DishServiceImpl extends BaseServiceImpl<DishMapper, DishEntity> implements DishService {

    private static final String DASHSCOPE_IMAGE_API = "/compatible-mode/v1/responses";
    private static final String DISH_IMAGE_SIZE = "1024*1024";

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishMaterialService dishMaterialService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private DishStepService dishStepService;
    @Resource(name = "dishVectorStore")
    private VectorStore dishVectorStore;
    @Resource(name = "qwen")
    private ChatModel qwenChatModel;

    @Value("${upload.path}")
    private String uploadPath;
    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;
    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com}")
    private String dashscopeBaseUrl;

    private final RestClient restClient = RestClient.builder().build();

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
    public void saveDishToVectorStore(DishEntity dishEntity) {

        List<Document> existingDocuments = dishVectorStore.similaritySearch(SearchRequest.builder().query("").filterExpression(new FilterExpressionBuilder().eq("dish_id", dishEntity.getId().toString()).build()).build());
        if (existingDocuments != null && !existingDocuments.isEmpty()){
            List<String> idsToDelete = existingDocuments.stream().map(Document::getId).collect(Collectors.toList());
            dishVectorStore.delete(idsToDelete);
        }

        /*List<DishMaterialEntity> materialEntityList = dishMaterialService.lambdaQuery().eq(DishMaterialEntity::getDishId, dishEntity.getId()).list();
        StringBuilder sb = new StringBuilder();
        sb.append(dishEntity.getName()).append(',');
        sb.append(CollUtil.join(materialEntityList.stream().map(DishMaterialEntity::getMaterialName).collect(Collectors.toList()), ","));*/


        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("dish_id", dishEntity.getId());
            metadata.put("dish_name", dishEntity.getName());
            dishVectorStore.add(List.of(new Document(dishEntity.getId().toString(), dishEntity.getName().toString(), metadata)));
        } catch (Exception e) {
            throw new OtherException("保存向量数据库失败", e);
        }

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

        List<DishFlavorEntity> existingFlavors = dishFlavorService.lambdaQuery().eq(DishFlavorEntity::getDishId, dishEntity.getId()).list();
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

        List<DishMaterialEntity> existingMaterials = dishMaterialService.lambdaQuery().eq(DishMaterialEntity::getDishId, dishEntity.getId()).list();
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

        List<DishStepEntity> existingSteps = dishStepService.lambdaQuery().eq(DishStepEntity::getDishId, dishEntity.getId()).list();
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

        this.saveDishToVectorStore(dishEntity);

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

        this.saveDishToVectorStore(dishEntity);
        return dishEntity;
    }

    @Override
    public String searchDishImageAndDownload(String dishName) {
        if (!StringUtils.hasText(dishName)) {
            return "";
        }

        try {
            String prompt = buildDishImagePrompt(dishName);
            String imageUrl = callDashScopeImageApi(dishName);
            if (!StringUtils.hasText(imageUrl)) {
                return "";
            }
            return downloadDishImage(imageUrl);
        } catch (Exception e) {
            log.warn("文搜图下载失败,dishName={}", dishName, e);
            return "";
        }
    }

    private String buildDishImagePrompt(String dishName) {
        String prompt = "请将以下菜名转换为文生图提示词，输出一行中文，突出成品摆盘、食材质感、暖色灯光、写实食物摄影，不要解释。菜名：" + dishName;
        try {
            String enhanced = qwenChatModel.call(prompt);
            if (StringUtils.hasText(enhanced)) {
                return enhanced.trim();
            }
        } catch (Exception e) {
            log.warn("菜品图片提示词增强失败，使用原始菜名,dishName={}", dishName, e);
        }
        return dishName.trim();
    }

    private String callDashScopeImageApi(String prompt) {

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen3.5-plus");
        requestBody.put("tools", JSONArray.of(JSONObject.of("type", "web_search_image")));
        requestBody.put("input", prompt);
        requestBody.put("parameters", JSONObject.of("size", DISH_IMAGE_SIZE, "n", 1));

        String responseBody = restClient.post()
                .uri(dashscopeBaseUrl + DASHSCOPE_IMAGE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + dashscopeApiKey)
                .body(requestBody.toJSONString())
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(responseBody)) {
            return "";
        }
        System.out.println(responseBody);
        JSONObject responseJson = JSONObject.parseObject(responseBody);
        JSONObject output = responseJson.getJSONObject("output");
        if (output == null) {
            return "";
        }
        JSONArray results = output.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            results = output.getJSONArray("images");
        }
        if (results == null || results.isEmpty()) {
            return "";
        }
        JSONObject first = results.getJSONObject(0);
        return first == null ? "" : first.getString("url");
    }

    private String downloadDishImage(String imageUrl) {
        ResponseEntity<byte[]> response = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .toEntity(byte[].class);
        byte[] imageBytes = response.getBody();
        if (imageBytes == null || imageBytes.length == 0) {
            return "";
        }

        String extension = resolveImageExtension(response.getHeaders().getContentType(), imageUrl);
        String relativePath = PathEnum.dish_img_path.getValue() + "/" + IdUtil.simpleUUID() + extension;
        String fullPath = uploadPath + relativePath;
        FileUtil.mkdir(uploadPath + PathEnum.dish_img_path.getValue());
        FileUtil.writeBytes(imageBytes, fullPath);
        return relativePath;
    }

    private String resolveImageExtension(MediaType mediaType, String imageUrl) {
        if (mediaType != null) {
            String subtype = mediaType.getSubtype();
            if (StringUtils.hasText(subtype)) {
                if ("jpeg".equalsIgnoreCase(subtype) || "jpg".equalsIgnoreCase(subtype)) {
                    return ".jpg";
                }
                if ("png".equalsIgnoreCase(subtype)) {
                    return ".png";
                }
                if ("webp".equalsIgnoreCase(subtype)) {
                    return ".webp";
                }
            }
        }
        if (StringUtils.hasText(imageUrl)) {
            String lower = imageUrl.toLowerCase(Locale.ROOT);
            if (lower.contains(".png")) {
                return ".png";
            }
            if (lower.contains(".webp")) {
                return ".webp";
            }
        }
        return ".jpg";
    }



    private void deleteRemovedFlavors(List<DishFlavorEntity> existingFlavors, Set<Long> retainFlavorIds) {
        List<Long> removeIds = existingFlavors.stream().map(DishFlavorEntity::getId).filter(id -> !retainFlavorIds.contains(id)).toList();
        if (!removeIds.isEmpty()) {
            dishFlavorService.removeByIds(removeIds);
        }
    }

    private void deleteRemovedMaterials(List<DishMaterialEntity> existingMaterials, Set<Long> retainMaterialIds) {
        List<Long> removeIds = existingMaterials.stream().map(DishMaterialEntity::getId).filter(id -> !retainMaterialIds.contains(id)).toList();
        if (!removeIds.isEmpty()) {
            dishMaterialService.removeByIds(removeIds);
        }
    }

    private void deleteRemovedSteps(List<DishStepEntity> existingSteps, Set<Long> retainStepIds) {
        List<Long> removeIds = existingSteps.stream().map(DishStepEntity::getId).filter(id -> !retainStepIds.contains(id)).toList();
        if (!removeIds.isEmpty()) {
            dishStepService.removeByIds(removeIds);
        }
    }
}
