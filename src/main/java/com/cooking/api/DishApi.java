package com.cooking.api;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.DishMaterialEntity;
import com.cooking.core.service.DishService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String dishName = params.getString("dishName");
        return ok(null);
    }

    /**
     * 按照提示词模板调用大模型生成指定菜品的制作信息
     * @param params
     * @return reactor.core.publisher.Flux<java.lang.String>
     * @author chenjc
     * @date 2026/3/10 01:06
     */
    @SneakyThrows
    @PostMapping("aigc")
    public Flux<String> aigc(@RequestBody JSONObject params) {
        String dishName = params.getString("dishName");

        String formatSuccess;
        String formatFail;
        try (InputStream successIns = aiSuccessResource.getInputStream();
             InputStream failIns = aiFailResource.getInputStream();){
            formatSuccess = IoUtil.read(successIns, StandardCharsets.UTF_8);
            formatFail = IoUtil.read(failIns, StandardCharsets.UTF_8);
        }
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemPromptTemplateMessage = systemPromptTemplate.createMessage(Map.of("format_success", formatSuccess, "format_fail", formatFail));

        PromptTemplate promptTemplate = new PromptTemplate(cookTemplate);
        Message message = promptTemplate.createMessage(Map.of("dishName", dishName));

        Prompt prompt = Prompt.builder().messages(List.of(systemPromptTemplateMessage, message)).build();

        return qwenChatModel.stream(prompt.toString());
    }

}
