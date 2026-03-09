package com.cooking.api;

import cn.hutool.core.io.IoUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("test")
public class TestApi {

    @Resource(name = "deepseek")
    private ChatModel deepseekChatModel;
    @Resource(name = "qwen")
    private ChatModel qwenChatModel;
    @Resource(name = "ollamaQwen")
    private ChatModel ollamaQwenChatModel;
    @Resource(name = "ollamaQwenClient")
    private ChatClient ollamaQwenClient;

    @Value("classpath:/template/user_prompt.md")
    private org.springframework.core.io.Resource cookTemplate;
    @Value("classpath:/template/system_prompt.md")
    private org.springframework.core.io.Resource systemPrompt;
    @Value("classpath:/template/ai_success.json5")
    private org.springframework.core.io.Resource aiSuccessResource;
    @Value("classpath:/template/ai_fail.json5")
    private org.springframework.core.io.Resource aiFailResource;
    @Resource(name = "qwenEmbedding")
    private EmbeddingModel qwenEmbedding;
    @Resource(name = "redisVectorStore")
    private VectorStore redisVectorStore;



    @GetMapping("test1")
    public Flux<String> test1(@RequestParam(defaultValue = "你好！") String msg) {
        return deepseekChatModel.stream(msg);
    }

    @GetMapping("test2")
    public Flux<String> test2(@RequestParam(defaultValue = "你好！") String msg) {
        return qwenChatModel.stream(msg);
    }

    @GetMapping("test3")
    public Flux<String> test3(@RequestParam(defaultValue = "你好！") String msg) {
        return ollamaQwenChatModel.stream(msg);
    }

    @GetMapping("test4")
    public Flux<String> test4(@RequestParam(defaultValue = "你好！") String msg) {
        ChatClient.ChatClientRequestSpec spec = ollamaQwenClient.prompt();
        spec.system("你是一名厨师，只能回答用户有关烹饪的问题，其他的领域的问题，提示无可奉告");
        spec.user(msg);
        return spec.stream().content();
    }

    @GetMapping("test5")
    public String test5(@RequestParam(defaultValue = "你好！") String msg) {
        List<Message> messages = new ArrayList<>();
        messages.add(SystemMessage.builder().text("你作为一名厨师，只能回答用户有关烹饪的问题，其他的领域的问题，提示无可奉告").build());
        messages.add(UserMessage.builder().text( msg).build());
        AssistantMessage output = ollamaQwenChatModel.call(Prompt.builder().messages(messages).build()).getResult().getOutput();
        return output.getText();
    }


    @GetMapping("test6")
    public Flux<String> test6(String course) {

        String formatSuccess;
        String formatFail;
        try (InputStream successIns = aiSuccessResource.getInputStream();
             InputStream failIns = aiFailResource.getInputStream();){
            formatSuccess = IoUtil.read(successIns, StandardCharsets.UTF_8);
            formatFail = IoUtil.read(failIns, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemPromptTemplateMessage = systemPromptTemplate.createMessage(Map.of("format_success", formatSuccess, "format_fail", formatFail));

        PromptTemplate promptTemplate = new PromptTemplate(cookTemplate);
        Message message = promptTemplate.createMessage(Map.of("course", course));

        Prompt prompt = Prompt.builder().messages(List.of(systemPromptTemplateMessage, message)).build();
        return qwenChatModel.stream(prompt.toString());

    }

    @GetMapping("test7")
    public String test7(@RequestParam(defaultValue = "厨师")String profession, @RequestParam(defaultValue = "烹饪") String work, String course, @RequestParam(defaultValue = "json") String format) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemPromptTemplateMessage = systemPromptTemplate.createMessage(Map.of("profession", profession, "work", work));

        PromptTemplate promptTemplate = new PromptTemplate(cookTemplate);
        Message message = promptTemplate.createMessage(Map.of("course", course, "format", format));

        Prompt prompt = Prompt.builder().messages(List.of(systemPromptTemplateMessage, message)).build();
        String content = ollamaQwenClient.prompt(prompt).call().content();
        return content;
    }

    @GetMapping("test8")
    public EmbeddingResponse test8(String course) {
        EmbeddingResponse embeddingResponse = qwenEmbedding.embedForResponse(List.of(course));
        Embedding result = qwenEmbedding.embedForResponse(List.of(course)).getResult();
        List<Embedding> results = qwenEmbedding.embedForResponse(List.of(course)).getResults();
        redisVectorStore.add(Arrays.asList(new Document(course)));
        return embeddingResponse;
    }


    @GetMapping("test9")
    public List<Document> test9(String course) {
        SearchRequest searchRequest = SearchRequest.builder().query(course).topK(2).build();
        List<Document> documents = redisVectorStore.similaritySearch(searchRequest);
        return documents;
    }

}
