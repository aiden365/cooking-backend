package com.cooking.api;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson2.JSONArray;
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
import com.cooking.utils.SystemContextHelper;
import jakarta.annotation.Resource;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户个性化菜谱 前端控制器
 * </p>
 *
 * @author aiden
 * @since 2026-04-09
 */
@RestController
@RequestMapping("user/individual/dish")
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
    @Resource(name = "ollamaQwen")
    private ChatModel qwenChatModel;

    @Value("classpath:/template/user_individual_prompt.md")
    private org.springframework.core.io.Resource userIndividualPrompt;
    @Value("classpath:/template/system_prompt.md")
    private org.springframework.core.io.Resource systemPrompt;
    @Value("classpath:/template/ai_success.json5")
    private org.springframework.core.io.Resource aiSuccessResource;
    @Value("classpath:/template/ai_fail.json5")
    private org.springframework.core.io.Resource aiFailResource;

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

    @PostMapping("aigc")
    public BaseResponse aigc(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        List<Long> labelIds = params.getList("labels", Long.class);
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

        String aiText = callAi(dishEntity, labels, materialList, flavorList, stepList);
        AIRecipeDTO aiRecipeDTO = parseAiRecipe(aiText);
        if ("error".equalsIgnoreCase(aiRecipeDTO.getStatus())) {
            throw new ApiException(BaseResponse.Code.fail.code, aiRecipeDTO.getMessage());
        }
        if (!"success".equalsIgnoreCase(aiRecipeDTO.getStatus())) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI生成失败");
        }

        String content = extractJsonText(aiText);
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
        return ok(entity);
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

    private String callAi(DishEntity dishEntity, List<LabelEntity> labels, List<DishMaterialEntity> materialList,List<DishFlavorEntity> flavorList, List<DishStepEntity> stepList) {
        Prompt prompt = buildPrompt(dishEntity, labels, materialList, flavorList, stepList);
        return qwenChatModel.call(prompt).getResult().getOutput().getText();
    }

    private Prompt buildPrompt(DishEntity dishEntity, List<LabelEntity> labels, List<DishMaterialEntity> materialList,
                               List<DishFlavorEntity> flavorList, List<DishStepEntity> stepList) {
        String formatSuccess;
        String formatFail;
        try (InputStream successIns = aiSuccessResource.getInputStream();
             InputStream failIns = aiFailResource.getInputStream()) {
            formatSuccess = IoUtil.read(successIns, StandardCharsets.UTF_8);
            formatFail = IoUtil.read(failIns, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ApiException(BaseResponse.Code.fail.code, "读取提示词模板失败");
        }

        Message systemMessage = new SystemPromptTemplate(systemPrompt)
                .createMessage(Map.of("format_success", formatSuccess, "format_fail", formatFail));
        Message userMessage = new PromptTemplate(userIndividualPrompt).createMessage(Map.of(
                "dishName", dishEntity.getName(),
                "dietaryPreference", buildDietaryPreferenceText(labels),
                "existMaterial", buildMaterialText(materialList),
                "existFlavor", buildFlavorText(flavorList),
                "existStep", buildStepText(stepList)
        ));
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

}
