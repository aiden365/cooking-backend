package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.*;
import com.cooking.core.service.DishService;
import com.cooking.core.service.DishFlavorService;
import com.cooking.core.service.DishMaterialService;
import com.cooking.core.service.DishStepService;
import com.cooking.core.service.LabelService;
import com.cooking.core.service.UserIndividualDishService;
import com.cooking.core.service.UserService;
import com.cooking.dto.AIRecipeDTO;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.AiResponseUtils;
import com.cooking.utils.SystemContextHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户个性化菜谱 前端控制器
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
@Slf4j
@RestController
@RequestMapping("individualDish")
public class UserIndividualDishApi extends BaseController {

    @Autowired
    private UserIndividualDishService userIndividualDishService;
    @Autowired
    private UserService userService;
    @Autowired
    private DishService dishService;
    @Autowired
    private DishMaterialService dishMaterialService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private DishStepService dishStepService;
    @Autowired
    private LabelService labelService;
    @Resource(name = "qwen")
    private ChatModel chatModel;

    @Value("classpath:/template/aigc_individual_prompt.md")
    private org.springframework.core.io.Resource userIndividualPrompt;
    @Value("classpath:/template/aigc_dish_system_prompt.md")
    private org.springframework.core.io.Resource systemPrompt;
    @Value("classpath:/template/aigc_dish_json_line.txt")
    private org.springframework.core.io.Resource dishJsonLine;
    @Value("classpath:/template/aigc_fail.json5")
    private org.springframework.core.io.Resource aiFailJson;
    @Autowired
    private VectorStore repositoryVectorStore;


    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        IPage<UserIndividualDishEntity> entityIPage = userIndividualDishService.findPage(new Page<>(pageNo, pageSize), params);

        Map<Long, DishEntity> dishEntityMap = dishService.findMapByIds(entityIPage.getRecords().stream().map(UserIndividualDishEntity::getDishId).collect(Collectors.toSet()));
        for (UserIndividualDishEntity entity : entityIPage.getRecords()) {
            DishEntity dishEntity = dishEntityMap.get(entity.getDishId());
            entity.setDishName(dishEntity == null ? "" : dishEntity.getName());
            entity.setDishImg(dishEntity == null ? "" : dishEntity.getImgPath());
        }

        return ok(entityIPage);
    }

    @RequestMapping("aigc")
    public Flux<String> aigc(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        List<Long> labelIds = params.getList("labelIds", Long.class);
        /*Long dishId = 13L;
        List<Long> labelIds = Arrays.asList(12L,13L,14L);*/
        if (!BaseEntity.validId(dishId)) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        if (CollectionUtils.isEmpty(labelIds)) {
            throw new ApiException(BaseResponse.Code.fail.code, "labels不能为空");
        }
        if (labelIds.size() > 3) {
            throw new ApiException(BaseResponse.Code.fail.code, "标签数量不能超过3个");
        }
        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        if (currentUser == null || !BaseEntity.validId(currentUser.getId())) {
            throw new ApiException(BaseResponse.Code.fail.code, "当前用户未登录");
        }

        DishEntity dishEntity = dishService.getById(dishId);
        if (dishEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜谱不存在");
        }

        Map<Long, LabelEntity> labelMap = labelService.findMapByIds(labelIds.stream().collect(Collectors.toSet()));
        List<LabelEntity> labels = labelIds.stream().map(labelMap::get).filter(label -> label != null && StringUtils.hasText(label.getLabelName())).toList();
        if (labels.isEmpty()) {
            throw new ApiException(BaseResponse.Code.fail.code, "未找到有效标签");
        }

        List<DishMaterialEntity> materialList = dishMaterialService.lambdaQuery().eq(DishMaterialEntity::getDishId, dishId).list();
        List<DishFlavorEntity> flavorList = dishFlavorService.lambdaQuery().eq(DishFlavorEntity::getDishId, dishId).list();
        List<DishStepEntity> stepList = dishStepService.lambdaQuery().eq(DishStepEntity::getDishId, dishId).orderByAsc(DishStepEntity::getSort).list();


        Consumer<StringBuilder> lineHandler = (aiFullResponse) -> {

            AIRecipeDTO aiRecipeDTO = AIRecipeDTO.parseAiRecipe(aiFullResponse.toString());
            if ("error".equalsIgnoreCase(aiRecipeDTO.getStatus())) {
                throw new ApiException(BaseResponse.Code.fail.code, aiRecipeDTO.getMessage());
            }
            if (!"success".equalsIgnoreCase(aiRecipeDTO.getStatus())) {
                throw new ApiException(BaseResponse.Code.fail.code, "AI生成失败");
            }

            String content = JSONObject.toJSONString(aiRecipeDTO);
            UserIndividualDishEntity entity = userIndividualDishService.lambdaQuery().eq(UserIndividualDishEntity::getUserId, currentUser.getId()).eq(UserIndividualDishEntity::getDishId, dishId).one();
            if (entity == null) {
                entity = new UserIndividualDishEntity();
                entity.setUserId(currentUser.getId());
                entity.setDishId(dishId);
            }
            entity.setContent(content);
            userIndividualDishService.saveOrUpdate(entity);

            entity.setDishName(dishEntity.getName());
            entity.setDishImg(dishEntity.getImgPath());
        };


        return callAi(lineHandler, dishEntity, labels, materialList, flavorList, stepList);
    }




    @PostMapping("detail")
    public BaseResponse detail(@RequestBody JSONObject params) {
        Long id = params.getLong("individualDishId");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }
        UserIndividualDishEntity entity = userIndividualDishService.getById(id);
        return ok(entity);
    }


    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long id = params.getLong("individualDishId");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }

        userIndividualDishService.removeById(id);
        return ok();
    }

    private Flux<String> callAi(Consumer<StringBuilder> complete, DishEntity dishEntity, List<LabelEntity> labels, List<DishMaterialEntity> materialList,List<DishFlavorEntity> flavorList, List<DishStepEntity> stepList) {
        Prompt prompt = buildPrompt(dishEntity, labels, materialList, flavorList, stepList);



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

        return lineFlux.concatWith(remainingFlux).doOnNext(line -> AiResponseUtils.appendLine(fullResponse, line)).doOnComplete(() -> complete.accept(fullResponse));
    }




    private Prompt buildPrompt(DishEntity dishEntity, List<LabelEntity> labels, List<DishMaterialEntity> materialList, List<DishFlavorEntity> flavorList, List<DishStepEntity> stepList) {
        String dishJsonLineString = null;

        try {
            dishJsonLineString = dishJsonLine.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String knowledgeContext = retrieveKnowledgeContext(null);


        Message systemMessage = new SystemPromptTemplate(systemPrompt).createMessage(Map.of("dishJSONLine", dishJsonLineString, "aiFailJson", JSONObject.of("type", "error","message", "错误原因")));
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("dishName", dishEntity.getName());
        userParams.put("preferences", buildDietaryPreferenceText(labels));
        userParams.put("materialsText", buildMaterialText(materialList));
        userParams.put("flavorsText", buildFlavorText(flavorList));
        userParams.put("stepsText", buildStepText(stepList));
        Message userMessage = new PromptTemplate(userIndividualPrompt).createMessage(userParams);

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

    private String buildDietaryPreferenceText(List<LabelEntity> labels) {
        return labels.stream()
                .map(LabelEntity::getLabelName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
    }

    private String buildMaterialText(List<DishMaterialEntity> materialList) {
        if (CollectionUtils.isEmpty(materialList)) {
            return "无";
        }
        return materialList.stream()
                .map(material -> "%s：%s%s".formatted(
                        defaultText(material.getMaterialName()),
                        defaultText(material.getDosage()),
                        StringUtils.hasText(material.getDeal()) ? "，处理方式：" + material.getDeal() : ""
                ))
                .collect(Collectors.joining("\n"));
    }

    private String buildFlavorText(List<DishFlavorEntity> flavorList) {
        if (CollectionUtils.isEmpty(flavorList)) {
            return "无";
        }
        return flavorList.stream()
                .map(flavor -> "%s：%s".formatted(defaultText(flavor.getFlavorName()), defaultText(flavor.getDosage())))
                .collect(Collectors.joining("\n"));
    }

    private String buildStepText(List<DishStepEntity> stepList) {
        if (CollectionUtils.isEmpty(stepList)) {
            return "无";
        }
        return stepList.stream()
                .map(step -> "%s. %s".formatted(step.getSort() == null ? 0 : step.getSort(), defaultText(step.getStepDescribe())))
                .collect(Collectors.joining("\n"));
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "无";
    }









}
