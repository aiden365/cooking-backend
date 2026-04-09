package com.cooking.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.*;
import com.cooking.core.service.*;
import com.cooking.dto.AIRecipeDTO;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.SystemContextHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Dish controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@Slf4j
@RestController
@RequestMapping("dish")
public class DishApi extends BaseController {

    @Autowired
    private DishService dishService;
    @Autowired
    private LabelService labelService;
    @Autowired
    private DishLableRelService dishLableRelService;
    @Autowired
    private UserDishCollectService userDishCollectService;
    @Autowired
    private UserShareService userShareService;



    @Resource(name = "ollamaQwen")
    private ChatModel qwenChatModel;
    @Resource(name = "dishVectorStore")
    private VectorStore dishVectorStore;
    @Resource(name = "repositoryVectorStore")
    private VectorStore repositoryVectorStore;

    @Value("classpath:/template/user_prompt.md")
    private org.springframework.core.io.Resource cookTemplate;
    @Value("classpath:/template/system_prompt.md")
    private org.springframework.core.io.Resource systemPrompt;
    @Value("classpath:/template/ai_success.json5")
    private org.springframework.core.io.Resource aiSuccessResource;
    @Value("classpath:/template/ai_fail.json5")
    private org.springframework.core.io.Resource aiFailResource;


    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        String search = params.getString("search");
        List<Long> dishIds = params.getList("dishId", Long.class);
        Long labelId = params.getLong("labelId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("dishIds", dishIds);
        queryParams.put("labelId", labelId);

        IPage<DishEntity> dishEntityPage = dishService.findPage(new Page<>(pageNo, pageSize), queryParams);
        List<Long> dishEntityIds = dishEntityPage.getRecords().stream().map(BaseEntity::getId).toList();

        Map<Long, DishLabelRelEntity> labelRelEntityMap = dishLableRelService.findMapByField(DishLabelRelEntity.Fields.dishId, dishEntityIds);
        Map<Long, LabelEntity> labelMap = labelService.findMapByIds(labelRelEntityMap.values().stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet()));
        labelRelEntityMap.values().stream().filter(rel -> labelMap.containsKey(rel.getLabelId())).forEach(rel -> {
            rel.setLabelName(labelMap.get(rel.getLabelId()).getLableName());
        });
        dishEntityPage.getRecords().forEach(dish -> {
            List<DishLabelRelEntity> relEntities = labelRelEntityMap.values().stream().filter(rel -> rel.getDishId().equals(dish.getId())).toList();
            dish.setLabelList(relEntities.stream().map(DishLabelRelEntity::getLabelName).toList());
        });

        Map<Long, UserDishCollectEntity> dishCollectMap = userDishCollectService.findMapByField(UserDishCollectEntity.Fields.dishId, dishEntityIds);
        dishEntityPage.getRecords().forEach(dish -> {
            dish.setCollectCount(dishCollectMap.values().stream().filter(collect -> collect.getDishId().equals(dish.getId())).count());
        });

        Map<Long, UserShareEntity> userShareMap = userShareService.findMapByField(UserShareEntity.Fields.dishId, dishEntityIds);
        dishEntityPage.getRecords().forEach(dish -> {
            dish.setShareCount(userShareMap.values().stream().filter(share -> share.getDishId().equals(dish.getId())).count());
        });


        return ok(dishEntityPage);
    }

    @PostMapping("search")
    public BaseResponse search(@RequestBody JSONObject params) {
        String search = params.getString("search");
        System.out.println( search);
        SearchRequest searchRequest = SearchRequest.builder().query(search).similarityThreshold(0.8f).topK(5).build();
        List<Document> documents = dishVectorStore.similaritySearch(searchRequest);
        return ok(documents);
    }

    @PostMapping("detail")
    public BaseResponse detail(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");

        if(!BaseEntity.validId(dishId)){
            return fail("菜谱ID不能为空");
        }

        DishEntity dishEntity = dishService.getById(dishId);
        Boolean userCollected = userDishCollectService.lambdaQuery().eq(UserDishCollectEntity::getDishId, dishId).eq(UserDishCollectEntity::getUserId, SystemContextHelper.getCurrentUser().getId()).count() > 0;
        Long collectCount = userDishCollectService.lambdaQuery().eq(UserDishCollectEntity::getDishId, dishId).count();
        Long shareCount = userShareService.lambdaQuery().eq(UserShareEntity::getDishId, dishId).count();
        dishEntity.setShareCount(shareCount);
        dishEntity.setCollectCount(collectCount);
        dishEntity.setUserCollected(userCollected);

        return ok();
    }

    /**
     * AI生成并保存菜谱
     */
    @PostMapping("aigc")
    public BaseResponse aigc(@RequestBody JSONObject params) {
        String dishName = params.getString("dishName");
        if (!StringUtils.hasText(dishName)) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishName不能为空");
        }

        String aiText = callAi(dishName);
        AIRecipeDTO aiRecipeDTO = parseAiRecipe(aiText);

        if ("error".equalsIgnoreCase(aiRecipeDTO.getStatus())) {
            throw new ApiException(BaseResponse.Code.fail.code, aiRecipeDTO.getMessage());
        }
        if (!"success".equalsIgnoreCase(aiRecipeDTO.getStatus())) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI生成失败");
        }
        if (!StringUtils.hasText(aiRecipeDTO.getDishName())) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI返回菜品名称为空");
        }

        DishEntity existed = dishService.lambdaQuery().eq(DishEntity::getName, aiRecipeDTO.getDishName()).one();
        if (existed != null) {
            return ok("菜品已存在，已跳过生成", existed);
        }

        // AI调用在事务外，避免慢调用占用事务。
        DishEntity savedDish = dishService.saveAigcRecipe(aiRecipeDTO);

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("dishId", savedDish.getId());
            metadata.put("dishName", savedDish.getName());
            dishVectorStore.add(List.of(new Document(savedDish.getId().toString(),buildVectorText(aiRecipeDTO), metadata)));
        } catch (Exception e) {
            log.error("保存向量数据库失败,dishId={}", savedDish.getId(), e);
        }

        return ok(savedDish);
    }

    private String callAi(String dishName) {
        Prompt prompt = buildPrompt(dishName, retrieveKnowledgeContext(dishName));
        return qwenChatModel.call(prompt).getResult().getOutput().getText();
    }

    private Prompt buildPrompt(String dishName, String knowledgeContext) {
        String formatSuccess;
        String formatFail;
        try (InputStream successIns = aiSuccessResource.getInputStream();
             InputStream failIns = aiFailResource.getInputStream()) {
            formatSuccess = IoUtil.read(successIns, StandardCharsets.UTF_8);
            formatFail = IoUtil.read(failIns, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ApiException(BaseResponse.Code.fail.code, "读取提示词模板失败");
        }

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("format_success", formatSuccess, "format_fail", formatFail));

        PromptTemplate promptTemplate = new PromptTemplate(cookTemplate);
        Message userMessage = promptTemplate.createMessage(Map.of("dishName", dishName));

        List<Message> messages = new ArrayList<>();
        messages.add(systemMessage);
        if (StringUtils.hasText(knowledgeContext)) {
            messages.add(new SystemMessage("""
                    ### 知识库参考资料
                    以下内容来自系统知识库检索结果，请优先参考其内容生成菜谱。
                    若检索内容不足或与食品安全常识冲突，请以食品安全和通用烹饪常识为准，不要编造不存在的专业结论。

                    %s
                    """.formatted(knowledgeContext)));
        }
        messages.add(userMessage);
        return Prompt.builder().messages(messages).build();
    }

    private String retrieveKnowledgeContext(String dishName) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(dishName)
                    .similarityThreshold(0.5f)
                    .filterExpression(new FilterExpressionBuilder().eq("type", 1).build())
                    .topK(8)
                    .build();
            List<Document> documents = repositoryVectorStore.similaritySearch(searchRequest);
            if (documents == null || documents.isEmpty()) {
                return "";
            }

            List<String> knowledgeList = documents.stream()
                    .filter(document -> document.getMetadata() != null && document.getMetadata().containsKey("repository_id"))
                    .sorted(Comparator.comparingInt(document -> repositorySort(document.getMetadata().get("type"))))
                    .map(Document::getText)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .stream()
                    .limit(4)
                    .toList();

            if (knowledgeList.isEmpty()) {
                return "";
            }

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < knowledgeList.size(); i++) {
                builder.append(i + 1).append(". ").append(knowledgeList.get(i)).append('\n');
            }
            return builder.toString();
        } catch (Exception e) {
            log.warn("知识库检索失败，已降级为普通生成,dishName={}", dishName, e);
            return "";
        }
    }

    private int repositorySort(Object type) {
        if (type == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(type.toString());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private AIRecipeDTO parseAiRecipe(String aiText) {
        String jsonText = extractJsonText(aiText);
        JSONObject root;
        try {
            root = JSONObject.parseObject(jsonText);
        } catch (Exception e) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI返回结果无法解析");
        }

        AIRecipeDTO dto = new AIRecipeDTO();
        dto.setStatus(root.getString("status"));
        dto.setDishName(root.getString("dish_name"));
        dto.setTakeTimes(root.getString("take_times"));
        dto.setTips(root.getString("tips"));
        dto.setMessage(root.getString("message"));

        JSONArray materials = root.getJSONArray("materials");
        if (materials != null) {
            dto.setMaterials(materials.toJavaList(AIRecipeDTO.Materials.class));
        }
        JSONArray flavors = root.getJSONArray("flavors");
        if (flavors != null) {
            dto.setFlavors(flavors.toJavaList(AIRecipeDTO.Flavors.class));
        }
        JSONArray steps = root.getJSONArray("steps");
        if (steps != null) {
            dto.setSteps(steps.toJavaList(AIRecipeDTO.Steps.class));
        }
        return dto;
    }

    private String extractJsonText(String aiText) {
        if (!StringUtils.hasText(aiText)) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI返回为空");
        }
        int start = aiText.indexOf('{');
        int end = aiText.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI返回格式不正确");
        }
        return aiText.substring(start, end + 1);
    }

    private String buildVectorText(AIRecipeDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append(dto.getDishName()).append(',');
        sb.append(CollUtil.join(dto.getMaterials().stream().map(AIRecipeDTO.Materials::getName).collect(Collectors.toList()), ","));
        return sb.toString();
    }

    @PostMapping("saveLabels")
    public BaseResponse saveLabels(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        List<Long> labelIds = params.getList("labels", Long.class);

        if (dishId == null) {
            return fail("dishId不能为空");
        }

        long currentLabelCount = dishLableRelService.count(new LambdaQueryWrapper<DishLabelRelEntity>().eq(DishLabelRelEntity::getDishId, dishId));
        Set<Long> existingLabelIds = dishLableRelService.list(new LambdaQueryWrapper<DishLabelRelEntity>().eq(DishLabelRelEntity::getDishId, dishId)).stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet());

        Set<Long> newLabelIds = (labelIds == null) ? Collections.emptySet() : new HashSet<>(labelIds);

        long toAddCount = newLabelIds.stream().filter(id -> !existingLabelIds.contains(id)).count();
        long toDeleteCount = existingLabelIds.stream().filter(id -> !newLabelIds.contains(id)).count();

        if (currentLabelCount + toAddCount - toDeleteCount > 5) {
            return fail("菜品标签总数不能超过5个");
        }

        dishLableRelService.saveDishLabels(dishId, labelIds);

        return ok("用户标签保存成功");
    }
}
