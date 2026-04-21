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
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.text.MessageFormat;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String IMAGE_DOWNLOAD_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36";
    private static final Duration DISH_IMAGE_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DISH_IMAGE_READ_TIMEOUT = Duration.ofSeconds(180);
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*]\\((https?://[^)\\s]+)\\)");

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
    @Resource(name = "qwenEmbedding")
    private EmbeddingModel qwenEmbeddingModel;
    @Autowired
    private JedisConnectionFactory jedisConnectionFactory;
    @Autowired
    private JedisPooled jedisPooled;


    @Value("${upload.path}")
    private String uploadPath;
    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;
    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com}")
    private String dashscopeBaseUrl;
    @Value("${spring.ai.dashscope.chat.options.model}")
    private String qwenModel;
    @Value("${spring.ai.vectorstore.redis.dish.prefix}")
    private String dishVectorStorePrefix;
    @Value("${spring.ai.vectorstore.redis.dish.index-name}")
    private String dishVectorStoreIndexName;

    private final RestClient restClient = createRestClient();

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
        /*FilterExpressionBuilder exprBuilder = new FilterExpressionBuilder();
        Filter.Expression expr = exprBuilder.eq("dish_id", dishEntity.getId()).build();
        deleteDishVectorDocuments(expr);*/

        List<DishMaterialEntity> materialEntityList = dishMaterialService.lambdaQuery().eq(DishMaterialEntity::getDishId, dishEntity.getId()).list();
        StringBuilder searchContent = new StringBuilder();
        searchContent.append(dishEntity.getName()).append(',');
        searchContent.append(CollUtil.join(materialEntityList.stream().map(DishMaterialEntity::getMaterialName).collect(Collectors.toList()), ","));


        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("dish_id", dishEntity.getId().toString());
            metadata.put("dish_name", dishEntity.getName());
            dishVectorStore.add(List.of(new Document(dishEntity.getId().toString(), searchContent.toString(), metadata)));
        } catch (Exception e) {
            throw new OtherException("保存向量数据库失败", e);
        }

    }

    private void deleteDishVectorDocuments(Filter.Expression expr) {
        try {
            dishVectorStore.delete(expr);
        } catch (Exception e) {
            if (isMissingDishVectorIndex(e)) {
                rebuildDishVectorStoreSchema();
                dishVectorStore.delete(expr);
                return;
            }
            throw e;
        }
    }

    @Override
    public Map<String, Object> rebuildAllVectorStore() {
        resetDishVectorStoreSchema();
        List<DishEntity> dishEntities = list();
        int rebuiltCount = 0;
        for (DishEntity dishEntity : dishEntities) {
            if (dishEntity == null || dishEntity.getId() == null || !StringUtils.hasText(dishEntity.getName())) {
                continue;
            }
            saveDishToVectorStore(dishEntity);
            rebuiltCount++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dishCount", dishEntities.size());
        result.put("rebuiltCount", rebuiltCount);
        return result;
    }

    @Override
    public Map<String, Object> diagnoseVectorStore() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("indexName", dishVectorStoreIndexName);
        result.put("prefix", dishVectorStorePrefix);

        Set<String> keys = jedisPooled.keys(dishVectorStorePrefix + "*");
        List<String> keyList = keys == null ? Collections.emptyList() : keys.stream().sorted().toList();
        result.put("redisKeyCount", keyList.size());
        result.put("sampleKeys", keyList.stream().limit(5).toList());

        try {
            Map<String, Object> indexInfo = jedisPooled.ftInfo(dishVectorStoreIndexName);
            result.put("indexInfo", simplifyIndexInfo(indexInfo));
        } catch (Exception e) {
            result.put("indexInfoError", e.getMessage());
        }

        List<Map<String, Object>> sampleDocuments = new ArrayList<>();
        for (String key : keyList.stream().limit(3).toList()) {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("key", key);
            String keyType = jedisPooled.type(key);
            sample.put("type", keyType);
            if ("hash".equalsIgnoreCase(keyType)) {
                Map<String, String> raw = jedisPooled.hgetAll(key);
                sample.put("fieldNames", raw.keySet());
                sample.put("hasContent", raw.containsKey("content") && StringUtils.hasText(raw.get("content")));
                sample.put("contentPreview", preview(raw.get("content")));
                sample.put("hasEmbedding", raw.containsKey("embedding") && StringUtils.hasText(raw.get("embedding")));
                sample.put("dishId", raw.get("dish_id"));
                sample.put("dishName", raw.get("dish_name"));
            }
            sampleDocuments.add(sample);
        }
        result.put("sampleDocuments", sampleDocuments);

        String diagnosticQuery = lambdaQuery()
                .select(DishEntity::getName)
                .last("limit 1")
                .list()
                .stream()
                .map(DishEntity::getName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("西红柿炒鸡蛋");
        result.put("diagnosticQuery", diagnosticQuery);

        SearchRequest searchRequest = SearchRequest.builder().query(diagnosticQuery).topK(5).build();
        List<Document> documents = dishVectorStore.similaritySearch(searchRequest);
        result.put("similarityResultCount", documents == null ? 0 : documents.size());
        result.put("similarityResultIds", documents == null ? Collections.emptyList() : documents.stream().map(Document::getId).toList());

        return result;
    }

    private void clearDishVectorStore() {
        String keyPattern = dishVectorStorePrefix + "*";
        Set<String> keys = jedisPooled.keys(keyPattern);
        if (keys != null && !keys.isEmpty()) {
            jedisPooled.del(keys.toArray(String[]::new));
        }
    }

    private void resetDishVectorStoreSchema() {
        try {
            jedisPooled.ftDropIndex(dishVectorStoreIndexName);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || !message.toLowerCase().contains("unknown index name")) {
                log.warn("删除菜谱向量索引失败，indexName={}", dishVectorStoreIndexName, e);
            }
        }

        clearDishVectorStore();
        rebuildDishVectorStoreSchema();
    }

    private void rebuildDishVectorStoreSchema() {
        RedisVectorStore redisVectorStore = RedisVectorStore.builder(jedisPooled, qwenEmbeddingModel)
                .initializeSchema(true)
                .indexName(dishVectorStoreIndexName)
                .prefix(dishVectorStorePrefix)
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("dish_id"),
                        RedisVectorStore.MetadataField.tag("dish_name"))
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
        redisVectorStore.afterPropertiesSet();
    }

    private boolean isMissingDishVectorIndex(Exception e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains("no such index")
                && message.contains(dishVectorStoreIndexName);
    }



    private Map<String, Object> simplifyIndexInfo(Map<String, Object> indexInfo) {
        if (indexInfo == null || indexInfo.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> simplified = new LinkedHashMap<>();
        copyIfPresent(indexInfo, simplified, "index_name");
        copyIfPresent(indexInfo, simplified, "num_docs");
        copyIfPresent(indexInfo, simplified, "num_terms");
        copyIfPresent(indexInfo, simplified, "hash_indexing_failures");
        copyIfPresent(indexInfo, simplified, "percent_indexed");
        copyIfPresent(indexInfo, simplified, "bytes_per_record_avg");
        return simplified;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private String preview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return content.length() <= 80 ? content : content.substring(0, 80) + "...";
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

        //搜索菜品图片
        new Thread(() -> {
            String imagePath = searchDishImageAndDownload(dishEntity.getName());
            if (StringUtils.hasText(imagePath)) {
                dishEntity.setImgPath(imagePath);
                super.updateById(dishEntity);
            }
        });
        return dishEntity;
    }

    @Override
    public String searchDishImageAndDownload(String dishName) {
        if (!StringUtils.hasText(dishName)) {
            return "";
        }

        try {
            String prompt = buildDishImagePrompt(dishName);
            List<String> imageUrls = callDashScopeImageApi(prompt);
            if (CollUtil.isEmpty(imageUrls)) {
                return "";
            }
            return downloadDishImage(imageUrls, dishName);
        } catch (Exception e) {
            log.warn("文搜图下载失败,dishName={}", dishName, e);
            return "";
        }
    }

    private String buildDishImagePrompt(String dishName) {
        return MessageFormat.format("帮我找一张{0}的菜品照片", dishName);
    }

    private RestClient createRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(DISH_IMAGE_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(DISH_IMAGE_READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private List<String> callDashScopeImageApi(String prompt) {

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", qwenModel);
        requestBody.put("tools", JSONArray.of(JSONObject.of("type", "web_search_image")));
        requestBody.put("input", prompt);

        String responseBody = restClient.post()
                .uri(dashscopeBaseUrl + DASHSCOPE_IMAGE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + dashscopeApiKey)
                .body(requestBody.toJSONString())
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(responseBody)) {
            return Collections.emptyList();
        }
        JSONObject responseJson = JSONObject.parseObject(responseBody);
        JSONArray outputs = responseJson.getJSONArray("output");
        if (outputs == null || outputs.isEmpty()) {
            log.warn("文搜图响应缺少output数组");
            return Collections.emptyList();
        }

        for (int i = 0; i < outputs.size(); i++) {
            JSONObject outputItem = outputs.getJSONObject(i);
            if (outputItem == null || !"completed".equalsIgnoreCase(outputItem.getString("status"))) {
                continue;
            }
            JSONArray contentArray = outputItem.getJSONArray("content");
            List<String> imageUrls = extractImageUrlsFromContent(contentArray);
            if (!imageUrls.isEmpty()) {
                return imageUrls;
            }
        }

        log.warn("文搜图响应中未找到status=completed且包含可下载图片地址的content");
        return Collections.emptyList();
    }

    private List<String> extractImageUrlsFromContent(JSONArray contentArray) {
        if (contentArray == null || contentArray.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < contentArray.size(); i++) {
            JSONObject contentItem = contentArray.getJSONObject(i);
            if (contentItem == null) {
                continue;
            }
            String text = contentItem.getString("text");
            if (!StringUtils.hasText(text)) {
                continue;
            }
            Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(text);
            while (matcher.find()) {
                imageUrls.add(matcher.group(1));
            }
        }
        return imageUrls;
    }

    private String downloadDishImage(List<String> imageUrls, String dishName) {
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            if (!StringUtils.hasText(imageUrl)) {
                log.warn("文搜图候选图片地址为空,dishName={},index={}", dishName, i);
                continue;
            }

            ResponseEntity<byte[]> response;
            try {
                response = restClient.get()
                        .uri(imageUrl)
                        .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                        .header("User-Agent", IMAGE_DOWNLOAD_USER_AGENT)
                        .retrieve()
                        .toEntity(byte[].class);
            } catch (Exception e) {
                log.warn("文搜图候选图片下载失败,dishName={},index={},url={}", dishName, i, imageUrl, e);
                continue;
            }

            MediaType contentType = response.getHeaders().getContentType();
            if (!isImageContentType(contentType)) {
                log.warn("文搜图候选地址响应非图片类型,dishName={},index={},url={},contentType={}", dishName, i, imageUrl, contentType);
                continue;
            }

            byte[] imageBytes = response.getBody();
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("文搜图候选图片内容为空,dishName={},index={},url={}", dishName, i, imageUrl);
                continue;
            }

            String extension = resolveImageExtension(contentType, imageUrl);
            String relativePath = PathEnum.dish_img_path.getValue() + "/" + IdUtil.simpleUUID() + extension;
            java.io.File targetDir = FileUtil.file(uploadPath, PathEnum.dish_img_path.getValue());
            FileUtil.mkdir(targetDir);
            java.io.File targetFile = FileUtil.file(uploadPath, relativePath);
            FileUtil.writeBytes(imageBytes, targetFile);
            if (!isValidDownloadedFile(targetFile)) {
                log.warn("文搜图候选图片落盘校验失败,dishName={},index={},url={},savedPath={}", dishName, i, imageUrl, targetFile.getAbsolutePath());
                FileUtil.del(targetFile);
                continue;
            }
            log.info("文搜图下载成功,dishName={},index={},url={},savedPath={}", dishName, i, imageUrl, targetFile.getAbsolutePath());
            return relativePath;
        }

        log.warn("文搜图所有候选地址均不可用,终止下载,dishName={}", dishName);
        return "";
    }

    private boolean isValidDownloadedFile(java.io.File targetFile) {
        return targetFile != null && targetFile.exists() && targetFile.isFile() && targetFile.length() > 0;
    }

    private boolean isImageContentType(MediaType mediaType) {
        return mediaType != null && "image".equalsIgnoreCase(mediaType.getType());
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
