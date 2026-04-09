package com.cooking.api;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.RandomUtil;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.utils.EmailUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping()
public class TestApi extends BaseController {

    @Resource(name = "deepseek")
    private ChatModel deepseekChatModel;
    @Resource(name = "qwen")
    private ChatModel qwenChatModel;
    @Resource(name = "ollamaQwen")
    private ChatModel ollamaQwenChatModel;
    @Resource(name = "ollamaQwenClient")
    private ChatClient ollamaQwenClient;

    @Value("classpath:/template/user_prompt.md")
    private org.springframework.core.io.Resource userTemplate;
    @Value("classpath:/template/user_individual_prompt.md")
    private org.springframework.core.io.Resource user1Template;

    @Value("classpath:/template/system_prompt.md")
    private org.springframework.core.io.Resource systemPrompt;

    @Value("classpath:/template/111.md")
    private org.springframework.core.io.Resource prompt111;
    @Value("classpath:/template/222.md")
    private org.springframework.core.io.Resource prompt222;


    @Value("classpath:/template/ai_success.json5")
    private org.springframework.core.io.Resource aiSuccessResource;
    @Value("classpath:/template/ai_fail.json5")
    private org.springframework.core.io.Resource aiFailResource;
    @Resource(name = "qwenEmbedding")
    private EmbeddingModel qwenEmbedding;
    @Resource(name = "dishVectorStore")
    private VectorStore dishVectorStore;
    @Autowired
    EmailUtils emailUtils;



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
    public Flux<String> test6(String course, String dietaryPreference) {

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

        PromptTemplate promptTemplate = new PromptTemplate(userTemplate);
        Message message = promptTemplate.createMessage(Map.of("dishName", course));

        Prompt prompt = Prompt.builder().messages(List.of(systemPromptTemplateMessage, message)).build();
        return qwenChatModel.stream(prompt.toString());

    }

    @GetMapping("test61")
    public Flux<String> test61(String course, String dietaryPreference) {

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

        String aa = "[{\"食材名\":\"青椒\",\"用量\":\"2个（约150g）\",\"处理方式\":\"去蒂、去籽，洗净后切成菱形片或细丝\"},{\"食材名\":\"鸡蛋\",\"用量\":\"3个\",\"处理方式\":\"打入碗中，加少许盐（分量外）和几滴清水（约5ml），充分搅打至蛋液起泡均匀\"}]";
        String bb = "[{\"调料名\":\"食用油\",\"用量\":\"25ml（约2汤匙）\"},{\"调料名\":\"盐\",\"用量\":\"3g（约1/2茶匙，含蛋液中预调部分）\"},{\"调料名\":\"白胡椒粉\",\"用量\":\"少许（可选，提鲜去腥）\"}]";
        String cc = "[{\"步骤序号\":1,\"步骤详细\":\"热锅冷油：将炒锅置于中大火上烧至微冒青烟（约180℃），倒入食用油，轻轻晃动锅体使油润满锅底。\"},{\"步骤序号\":2,\"步骤详细\":\"滑炒鸡蛋：油温升高后（插入筷子周围有密集小泡），倒入蛋液，待边缘开始凝固时用锅铲快速划散，保持嫩滑不焦糊，约7–8成熟即盛出备用（勿过度翻炒）。\"},{\"步骤序号\":3,\"步骤详细\":\"爆香青椒：锅中留底油（如不足可补5ml），转中火，下青椒片，大火快煸1–1.5分钟，至表皮略起虎皮褶皱、香气溢出、颜色更鲜亮。\"},{\"步骤序号\":4,\"步骤详细\":\"合炒调味：将炒好的鸡蛋倒回锅中，与青椒轻翻均匀，撒入剩余盐和白胡椒粉，快速翻炒10–15秒，使味道融合、鸡蛋完全熟透但依然蓬松湿润。\"},{\"步骤序号\":5,\"步骤详细\":\"出锅装盘：关火，立即盛入预热过的盘中，避免余热导致鸡蛋变老。\"}]";
        PromptTemplate promptTemplate = new PromptTemplate(user1Template);
        Message message = promptTemplate.createMessage(Map.of("dishName", course, "dietaryPreference", dietaryPreference, "existMaterial", aa, "existFlavor", bb, "existStep", cc));

        Prompt prompt = Prompt.builder().messages(List.of(systemPromptTemplateMessage, message)).build();
        return qwenChatModel.stream(prompt.toString());

    }


    @GetMapping("test62")
    public Flux<String> test62() {

        String formatSuccess;
        String formatFail;
        try (InputStream successIns = aiSuccessResource.getInputStream();
             InputStream failIns = aiFailResource.getInputStream();){
            formatSuccess = IoUtil.read(successIns, StandardCharsets.UTF_8);
            formatFail = IoUtil.read(failIns, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String aa = "{\"status\":\"success/error\",\"daily_analysis\":\"简述今日配餐的营养逻辑，特别是针对当前身体状态的调整说明\",\"meals\":{\"breakfast\":{\"name\":\"菜品名称\",\"reason\":\"推荐理由（结合营养目标或身体状态）\",\"nutrients\":\"预估主要营养成分（碳水/蛋白质/热量）\"},\"lunch\":{\"name\":\"菜品名称\",\"reason\":\"推荐理由\",\"nutrients\":\"预估主要营养成分\"},\"dinner\":{\"name\":\"菜品名称\",\"reason\":\"推荐理由\",\"nutrients\":\"预估主要营养成分\"}},\"error_analysis\":\"\"}";

        String u1 = "{\"年龄\":\"27\",\"身高\":\"175\",\"体重\":\"60kg\"}";
        String u2 = "{\"蛋白质\":\"75g\",\"碳水化合物\":\"275g\",\"脂肪\":\"67g\"}";
        String u3 = "[\"西红柿炒鸡蛋\", \"鸡蛋炒河粉\", \"青椒肉丝盖浇饭\",\"粉丝包\",\"糖醋排骨\",\"香菇青菜\"]";
        String u4 = "[\"感冒\", \"发烧\"]";
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(prompt111);
        Message systemPromptTemplateMessage = systemPromptTemplate.createMessage(Map.of("format_success", aa));

        PromptTemplate promptTemplate = new PromptTemplate(prompt222);
        Message message = promptTemplate.createMessage(Map.of("userInfo", u1,"goals", u2,"history", u3,"status", u4));

        Prompt prompt = Prompt.builder().messages(List.of(systemPromptTemplateMessage, message)).build();
        return qwenChatModel.stream(prompt.toString());

    }

    @GetMapping("test7")
    public String test7(@RequestParam(defaultValue = "厨师")String profession, @RequestParam(defaultValue = "烹饪") String work, String course, @RequestParam(defaultValue = "json") String format) {
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemPromptTemplateMessage = systemPromptTemplate.createMessage(Map.of("profession", profession, "work", work));

        PromptTemplate promptTemplate = new PromptTemplate(userTemplate);
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
        dishVectorStore.add(Arrays.asList(new Document(course)));
        return embeddingResponse;
    }


    @GetMapping("test9")
    public List<Document> test9(String course) {
        SearchRequest searchRequest = SearchRequest.builder().query(course).similarityThreshold(0.8f).topK(2).build();
        List<Document> documents = dishVectorStore.similaritySearch(searchRequest);
        return documents;
    }

    @GetMapping("test10")
    public BaseResponse test10() {

        String randomNumbers = RandomUtil.randomNumbers(6);
        emailUtils.sendVerificationCodeEmail("chenjiacheng365@qq.com", randomNumbers, 5);
        return ok();
    }



}
