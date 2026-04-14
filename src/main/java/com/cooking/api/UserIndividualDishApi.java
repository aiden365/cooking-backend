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
    private ChatModel qwenChatModel;
    @Resource(name = "ollamaQwen")
    private ChatModel ollamaQwen;

    @Value("classpath:/template/dish_individual_prompt.md")
    private org.springframework.core.io.Resource userIndividualPrompt;
    @Value("classpath:/template/system_prompt.md")
    private org.springframework.core.io.Resource systemPrompt;
    @Value("classpath:/template/dish_json_line.txt")
    private org.springframework.core.io.Resource dishJsonLine;
    @Value("classpath:/template/ai_fail.json5")
    private org.springframework.core.io.Resource aiFailJson;





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


    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
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

        Flux<String> lineFlux = ollamaQwen.stream(prompt).map(AiResponseUtils::extractChunkText).handle((String chunk, SynchronousSink<String> sink) -> AiResponseUtils.appendAndEmitCompleteLines(buffer, chunk, sink));

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
        String aiFailJsonString = null;

        try {
            dishJsonLineString = dishJsonLine.getContentAsString(StandardCharsets.UTF_8);
            aiFailJsonString = aiFailJson.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Message systemMessage = new SystemPromptTemplate(systemPrompt).createMessage(Map.of("dishJSONLine", dishJsonLineString, "aiFailJson", aiFailJsonString));
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("dishName", dishEntity.getName());
        userParams.put("preferences", buildDietaryPreferenceText(labels));
        userParams.put("materialsText", buildMaterialText(materialList));
        userParams.put("flavorsText", buildFlavorText(flavorList));
        userParams.put("stepsText", buildStepText(stepList));
        Message userMessage = new PromptTemplate(userIndividualPrompt).createMessage(userParams);

        return Prompt.builder().messages(List.of(systemMessage, userMessage)).build();
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
