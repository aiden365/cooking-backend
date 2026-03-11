package com.cooking.api;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.service.DishService;
import com.cooking.dto.AIRecipeDTO;
import com.cooking.exceptions.ApiException;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Resource(name = "qwen")
    private ChatModel qwenChatModel;
    @Resource(name = "qwenEmbedding")
    private EmbeddingModel qwenEmbedding;
    @Resource(name = "redisVectorStore")
    private VectorStore redisVectorStore;

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
        Long dishId = params.getLong("dishId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("dishId", dishId);

        IPage<DishEntity> entityIPage = dishService.findPage(new Page<>(pageNo, pageSize), queryParams);
        return ok(entityIPage);
    }

    @PostMapping("search")
    public BaseResponse search(@RequestBody JSONObject params) {
        String search = params.getString("search");
        SearchRequest searchRequest = SearchRequest.builder().query(search).topK(2).build();
        List<Document> documents = redisVectorStore.similaritySearch(searchRequest);
        return ok(documents);
    }

    /**
     * AI生成并保存菜谱
     */
    @PostMapping("aigcSave")
    public BaseResponse aigcSave(@RequestBody JSONObject params) {
        String dishName = params.getString("dishName");
        if (!StringUtils.hasText(dishName)) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishName不能为空");
        }

        String aiText = callAi(dishName);
        AIRecipeDTO aiRecipeDTO = parseAiRecipe(aiText);

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
            redisVectorStore.add(List.of(new Document(buildVectorText(aiRecipeDTO), metadata)));
        } catch (Exception e) {
            log.error("保存向量数据库失败,dishId={}", savedDish.getId(), e);
        }

        return ok(savedDish);
    }

    private String callAi(String dishName) {
        Prompt prompt = buildPrompt(dishName);
        return qwenChatModel.call(prompt).getResult().getOutput().getText();
    }

    private Prompt buildPrompt(String dishName) {
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

        return Prompt.builder().messages(List.of(systemMessage, userMessage)).build();
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
        sb.append("name:").append(dto.getDishName()).append('\n');
        sb.append("materials:").append(CollUtil.join(dto.getMaterials().stream().map(e -> e.getName()).collect(Collectors.toList()), ",")).append('\n');
        return sb.toString();
    }
}
