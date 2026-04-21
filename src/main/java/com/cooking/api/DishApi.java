package com.cooking.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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
import com.cooking.dto.DishSaveDTO;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.AiResponseUtils;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Dish controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
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
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private DishMaterialService dishMaterialService;
    @Autowired
    private DishStepService dishStepService;
    @Autowired
    private DishAppraisesService dishAppraisesService;
    @Autowired
    private DishCommentService dishCommentService;
    @Autowired
    private UserShareCommentService userShareCommentService;
    @Autowired
    private UserIndividualDishService userIndividualDishService;
    @Autowired
    private UserDietRecordService userDietRecordService;



    @Resource(name = "qwen")
    private ChatModel chatModel;
    @Resource(name = "ollamaQwen")
    private ChatModel ollamaQwen;

    @Resource(name = "dishVectorStore")
    private VectorStore dishVectorStore;
    @Resource(name = "repositoryVectorStore")
    private VectorStore repositoryVectorStore;

    @Value("classpath:/template/aigc_dish_prompt.md")
    private org.springframework.core.io.Resource dishPrompt;
    @Value("classpath:/template/aigc_dish_system_prompt.md")
    private org.springframework.core.io.Resource systemPrompt;
    @Value("classpath:/template/aigc_dish_json_line.txt")
    private org.springframework.core.io.Resource dishJsonLine;
    @Value("classpath:/template/aigc_fail.json5")
    private org.springframework.core.io.Resource aiFailJson;


    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        String search = params.getString("search");
        List<Long> dishIds = params.getList("dishId", Long.class);
        Long labelId = params.getLong("labelId");
        Integer checkStatus = params.getInteger("checkStatus");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("dishIds", dishIds);
        queryParams.put("labelId", labelId);
        queryParams.put("checkStatus", checkStatus);

        IPage<DishEntity> dishEntityPage = dishService.findPage(new Page<>(pageNo, pageSize), queryParams);
        List<Long> dishEntityIds = dishEntityPage.getRecords().stream().map(BaseEntity::getId).toList();
        if (dishEntityIds.isEmpty()) {
            return ok(dishEntityPage);
        }

        Map<Long, DishLabelRelEntity> labelRelEntityMap = dishLableRelService.findMapByField(DishLabelRelEntity.Fields.dishId, dishEntityIds);
        Map<Long, LabelEntity> labelMap = labelService.findMapByIds(labelRelEntityMap.values().stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet()));
        labelRelEntityMap.values().stream().filter(rel -> labelMap.containsKey(rel.getLabelId())).forEach(rel -> {
            rel.setLabelName(labelMap.get(rel.getLabelId()).getLabelName());
        });
        dishEntityPage.getRecords().forEach(dish -> {
            List<DishLabelRelEntity> relEntities = labelRelEntityMap.values().stream().filter(rel -> rel.getDishId().equals(dish.getId())).toList();
            dish.setLabelNames(relEntities.stream().map(DishLabelRelEntity::getLabelName).toList());
        });

        Map<Long, UserDishCollectEntity> dishCollectMap = userDishCollectService.findMapByField(UserDishCollectEntity.Fields.dishId, dishEntityIds);
        dishEntityPage.getRecords().forEach(dish -> {
            dish.setCollectCount(dishCollectMap.values().stream().filter(collect -> collect.getDishId().equals(dish.getId())).count());
        });

        Map<Long, UserShareEntity> userShareMap = userShareService.findMapByField(UserShareEntity.Fields.dishId, dishEntityIds);
        dishEntityPage.getRecords().forEach(dish -> {
            dish.setShareCount(userShareMap.values().stream().filter(share -> share.getDishId().equals(dish.getId())).count());
        });

        Map<Long, Long> flavorCountMap = dishFlavorService.lambdaQuery().in(DishFlavorEntity::getDishId, dishEntityIds).list().stream().collect(Collectors.groupingBy(DishFlavorEntity::getDishId, Collectors.counting()));
        Map<Long, Long> materialCountMap = dishMaterialService.lambdaQuery().in(DishMaterialEntity::getDishId, dishEntityIds).list().stream().collect(Collectors.groupingBy(DishMaterialEntity::getDishId, Collectors.counting()));
        Map<Long, Long> stepCountMap = dishStepService.lambdaQuery().in(DishStepEntity::getDishId, dishEntityIds).list().stream().collect(Collectors.groupingBy(DishStepEntity::getDishId, Collectors.counting()));
        dishEntityPage.getRecords().forEach(dish -> {
            dish.setFlavorCount(flavorCountMap.getOrDefault(dish.getId(), 0L).intValue());
            dish.setMaterialCount(materialCountMap.getOrDefault(dish.getId(), 0L).intValue());
            dish.setStepCount(stepCountMap.getOrDefault(dish.getId(), 0L).intValue());
        });

        return ok(dishEntityPage);
    }

    @PostMapping("search")
    public BaseResponse search(@RequestBody JSONObject params) {
        String search = params.getString("search");
        SearchRequest searchRequest = SearchRequest.builder().query(search).topK(5).build();
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
        List<DishFlavorEntity> flavorEntityList = dishFlavorService.lambdaQuery().eq(DishFlavorEntity::getDishId, dishId).list();
        List<DishMaterialEntity> materialEntityList = dishMaterialService.lambdaQuery().eq(DishMaterialEntity::getDishId, dishId).list();
        List<DishStepEntity> stepEntityList = dishStepService.lambdaQuery().eq(DishStepEntity::getDishId, dishId).orderByAsc(DishStepEntity::getSort).list();
        int flavorCount = flavorEntityList.size();
        int materialCount = materialEntityList.size();
        int stepCount = stepEntityList.size();
        List<DishLabelRelEntity> relList = dishLableRelService.lambdaQuery().eq(DishLabelRelEntity::getDishId, dishId).list();
        List<LabelEntity> labelEntityList = labelService.listByIds(relList.stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet()));

        dishEntity.setShareCount(shareCount);
        dishEntity.setCollectCount(collectCount);
        dishEntity.setUserCollected(userCollected);
        dishEntity.setFlavorCount(flavorCount);
        dishEntity.setMaterialCount(materialCount);
        dishEntity.setStepCount(stepCount);
        dishEntity.setLabelNames(labelEntityList.stream().map(LabelEntity::getLabelName).toList());
        dishEntity.setFlavorList(flavorEntityList);
        dishEntity.setMaterialList(materialEntityList);
        dishEntity.setStepList(stepEntityList);

        return ok(dishEntity);
    }

    @PostMapping("labels")
    public BaseResponse labels(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        if (!BaseEntity.validId(dishId)) {
            return fail("dishId不能为空");
        }

        List<DishLabelRelEntity> relList = dishLableRelService.lambdaQuery().eq(DishLabelRelEntity::getDishId, dishId).list();
        if (relList.isEmpty()) {
            return ok(Collections.emptyList());
        }

        List<LabelEntity> labelEntityList = labelService.listByIds(relList.stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet()));

        return ok(labelEntityList);
    }



    @PostMapping("delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(@RequestBody JSONObject params) {
        List<Long> ids = params.getList("ids", Long.class);
        if (ids == null || ids.isEmpty()) {
            return fail("ids不能为空");
        }

        List<UserShareEntity> shareList = userShareService.lambdaQuery().in(UserShareEntity::getDishId, ids).list();
        List<Long> shareIds = shareList.stream().map(UserShareEntity::getId).toList();

        if (!shareIds.isEmpty()) {
            userShareCommentService.lambdaUpdate().in(UserShareCommentEntity::getUserShareId, shareIds).remove();
        }

        userShareService.lambdaUpdate().in(UserShareEntity::getDishId, ids).remove();
        userDishCollectService.lambdaUpdate().in(UserDishCollectEntity::getDishId, ids).remove();
        userIndividualDishService.lambdaUpdate().in(UserIndividualDishEntity::getDishId, ids).remove();
        userDietRecordService.lambdaUpdate().in(UserDietRecordEntity::getDishId, ids).remove();
        dishAppraisesService.lambdaUpdate().in(DishAppraisesEntity::getDishId, ids).remove();
        dishCommentService.lambdaUpdate().in(DishCommentEntity::getDishId, ids).remove();
        dishLableRelService.lambdaUpdate().in(DishLabelRelEntity::getDishId, ids).remove();
        dishMaterialService.lambdaUpdate().in(DishMaterialEntity::getDishId, ids).remove();
        dishFlavorService.lambdaUpdate().in(DishFlavorEntity::getDishId, ids).remove();
        dishStepService.lambdaUpdate().in(DishStepEntity::getDishId, ids).remove();

        List<Document> dishDocuments = new ArrayList<>();
        for (Long dishId : ids) {
            List<Document> documents = dishVectorStore.similaritySearch(SearchRequest.builder().query("").filterExpression(new FilterExpressionBuilder().eq("dishId", dishId.toString()).build()).build());
            if (documents != null && !documents.isEmpty()) {
                dishDocuments.addAll(documents);
            }
        }
        if (!dishDocuments.isEmpty()) {
            dishVectorStore.delete(dishDocuments.stream().map(Document::getId).toList());
        }

        dishService.removeByIds(ids);
        return ok();
    }

    @PostMapping("save")
    public BaseResponse save(@RequestBody DishSaveDTO dishSaveDTO) {
        if (!StringUtils.hasText(dishSaveDTO.getName())) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜名不能为空");
        }
        if (!StringUtils.hasText(dishSaveDTO.getTakeTimes())) {
            throw new ApiException(BaseResponse.Code.fail.code, "预计用时不能为空");
        }
        if (dishSaveDTO.getCheckStatus() == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "审核状态不能为空");
        }

        DishEntity dishEntity;
        try {
            dishEntity = dishService.saveDish(dishSaveDTO);
        } catch (IllegalArgumentException e) {
            throw new ApiException(BaseResponse.Code.fail.code, e.getMessage());
        }
        return ok(dishEntity);
    }

    @PostMapping("verifyName")
    public BaseResponse verifyName(@RequestBody JSONObject params) {
        String dishName = params.getString("dishName");
        if(StrUtil.isNotBlank(dishName)){
            String res = ollamaQwen.call(MessageFormat.format("请验证下这是否是一个合法的菜名，只需回答是或不是。名称为：{0}", dishName));
        }
        return ok(true);
    }

    /**
     * AI生成并保存菜谱
     */
    @RequestMapping("aigc")
    public Flux<String> aigc(String dishName) {
        /*String dishName = params.getString("dishName");*/
        if (!StringUtils.hasText(dishName)) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishName不能为空");
        }

        Function<StringBuilder, String> complete = (aiFullResponse) -> {
            AIRecipeDTO aiRecipeDTO = AIRecipeDTO.parseAiRecipe(aiFullResponse.toString());
            if ("error".equalsIgnoreCase(aiRecipeDTO.getStatus())) {
                throw new ApiException(BaseResponse.Code.fail.code, aiRecipeDTO.getMessage());
            }
            if (!"success".equalsIgnoreCase(aiRecipeDTO.getStatus())) {
                throw new ApiException(BaseResponse.Code.fail.code, "AI生成失败");
            }
            // AI调用在事务外，避免慢调用占用事务。
            DishEntity dishEntity = dishService.saveAigcRecipe(aiRecipeDTO);
            return buildSavedMessage(dishEntity.getId());
        };

        return callAi(dishName, complete);
    }

    private Flux<String> callAi(String dishName, java.util.function.Function<StringBuilder, String> complete) {
        String knowledgeContext = retrieveKnowledgeContext(dishName);
        Prompt prompt = buildPrompt(dishName, knowledgeContext);

        StringBuilder buffer = new StringBuilder();
        StringBuilder fullResponse = new StringBuilder();

        Flux<String> lineFlux = chatModel.stream(prompt).map(AiResponseUtils::extractChunkText).handle((String chunk, SynchronousSink<String> sink) -> AiResponseUtils.appendAndEmitCompleteLines(buffer, chunk, sink));

        Flux<String> remainingFlux = Flux.defer(() -> {
            String lastLine = buffer.toString().trim();
            if (lastLine.isEmpty()) {
                return Flux.empty();
            }
            if (AiResponseUtils.isCompleteJsonObject(lastLine)) {
                buffer.setLength(0);
                return Flux.just(lastLine);
            }
            return Flux.empty();
        });

        Flux<String> responseFlux = lineFlux.concatWith(remainingFlux).doOnNext(line -> AiResponseUtils.appendLine(fullResponse, line));

        Mono<String> savedFlux = Mono.fromCallable(() -> complete.apply(fullResponse)).filter(StringUtils::hasText);

        return responseFlux.concatWith(savedFlux);

    }

    private String buildSavedMessage(Long dishId) {
        return JSONObject.of("type", "saved","data", JSONObject.of("dishId", dishId)).toJSONString();
    }


    private Prompt buildPrompt(String dishName, String knowledgeContext) {
        String dishJsonLineString = null;

        try {
            dishJsonLineString = dishJsonLine.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Message systemMessage = new SystemPromptTemplate(systemPrompt).createMessage(Map.of("dishJSONLine", dishJsonLineString, "aiFailJson", JSONObject.of("type", "error","简短的几个字说明生成失败的原因", "错误原因"), "knowledgeContext", StringUtils.hasText(knowledgeContext) ? knowledgeContext : "无"));
        PromptTemplate promptTemplate = new PromptTemplate(dishPrompt);
        Message userMessage = promptTemplate.createMessage(Map.of("dishName", dishName));
        return Prompt.builder().messages(List.of(systemMessage, userMessage)).build();
    }

    private String retrieveKnowledgeContext(String dishName) {
        try {
            SearchRequest searchRequest = SearchRequest.builder().query(dishName).similarityThreshold(0.8f).filterExpression(new FilterExpressionBuilder().eq("type", 1).build()).topK(8).build();
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



    @PostMapping("addLabel")
    public BaseResponse addLabel(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        Long labelId = params.getLong("labelId");

        if (dishId == null) {
            return fail("dishId不能为空");
        }

        if (labelId == null) {
            return fail("标签不能为空");
        }

        DishLabelRelEntity dishLabelRelEntity = dishLableRelService.lambdaQuery().eq(DishLabelRelEntity::getDishId, dishId).eq(DishLabelRelEntity::getLabelId, labelId).list().stream().findAny().orElse(null);
        if (dishLabelRelEntity != null) {
            return fail("该标签已选择");
        }

        long currentLabelCount = dishLableRelService.count(new LambdaQueryWrapper<DishLabelRelEntity>().eq(DishLabelRelEntity::getDishId, dishId));
        if (currentLabelCount > 4) {
            return fail("菜品标签总数不能超过4个");
        }



        DishLabelRelEntity build = DishLabelRelEntity.builder().dishId(dishId).labelId(labelId).build();
        dishLableRelService.save(build);
        return ok("用户标签保存成功");
    }


    @PostMapping("delLabel")
    public BaseResponse delLabel(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        Long labelId = params.getLong("labelId");

        if (dishId == null) {
            return fail("dishId不能为空");
        }

        if (labelId == null) {
            return fail("标签不能为空");
        }

        DishLabelRelEntity dishLabelRelEntity = dishLableRelService.lambdaQuery().eq(DishLabelRelEntity::getDishId, dishId).eq(DishLabelRelEntity::getLabelId, labelId).list().stream().findAny().orElse(null);
        if (dishLabelRelEntity == null) {
            return ok();
        }
        dishLableRelService.removeById(dishLabelRelEntity.getId());
        return ok("用户标签保存成功");
    }
}
