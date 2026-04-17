package com.cooking.api;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.*;
import com.cooking.core.service.*;
import com.cooking.core.service.impl.NutritionServiceImpl;
import com.cooking.core.service.impl.UserLabelRelServiceImpl;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.AiResponseUtils;
import com.cooking.utils.SystemContextHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * User diet record controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Slf4j
@RestController
@RequestMapping("diet")
public class UserDietRecordApi extends BaseController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int NUTRITION_CONTEXT_LIMIT = 8;

    @Autowired
    private UserDietRecordService userDietRecordService;
    @Autowired
    private UserService userService;
    @Autowired
    private DishService dishService;
    @Autowired
    private DishLableRelService dishLableRelService;
    @Autowired
    private LabelService labelService;
    @Autowired
    private UserShareService userShareService;
    @Autowired
    private UserDishCollectService userDishCollectService;
    @Value("classpath:/template/aigc_diet_system_prompt.md")
    private org.springframework.core.io.Resource aigcDietSystemPrompt;
    @Value("classpath:/template/aigc_diet_user_prompt.md")
    private org.springframework.core.io.Resource aigcDietUserPrompt;

    @Resource(name = "qwen")
    private ChatModel chatModel;
    @Resource(name = "repositoryVectorStore")
    private VectorStore repositoryVectorStore;

    @Value("classpath:/template/aigc_diet_json_line.txt")
    private org.springframework.core.io.Resource aigcDietJsonLine;
    @Autowired
    private UserNutritionRelService userNutritionRelService;
    @Autowired
    private NutritionServiceImpl nutritionService;
    @Autowired
    private UserLabelRelServiceImpl userLabelRelService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        IPage<UserDietRecordEntity> entityIPage = userDietRecordService.findPage(new Page<>(pageNo, pageSize), params);
        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(UserDietRecordEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, DishEntity> dishEntityMap = dishService.findMapByIds(entityIPage.getRecords().stream().map(UserDietRecordEntity::getDishId).collect(Collectors.toSet()));
        entityIPage.getRecords().forEach(e -> {
            UserEntity userEntity = userEntityMap.get(e.getUserId());
            DishEntity dishEntity = dishEntityMap.get(e.getDishId());
            e.setUserName(userEntity == null ? "" : userEntity.getUserName());
            e.setDishName(dishEntity == null ? "" : dishEntity.getName());
        });

        return ok(entityIPage);
    }

    @PostMapping("day-group")
    public BaseResponse dayGroup() {


        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        List<UserDishCollectEntity> collectList = userDishCollectService.lambdaQuery().eq(UserDishCollectEntity::getUserId, userId).orderByDesc(UserDishCollectEntity::getCreateTime).list();
        if (collectList.isEmpty()) {
            return ok(Collections.emptyList());
        }

        List<Long> dishIds = collectList.stream().map(UserDishCollectEntity::getDishId).filter(Objects::nonNull).distinct().toList();
        Map<Long, DishEntity> dishMap = dishService.findMapByIds(Set.copyOf(dishIds));
        Map<Long, Long> collectTotalNumMap = userDishCollectService.lambdaQuery().in(UserDishCollectEntity::getDishId, dishIds).list().stream().collect(Collectors.groupingBy(UserDishCollectEntity::getDishId, Collectors.counting()));

        Map<Long, DishLabelRelEntity> dishLabelRelMap = dishLableRelService.findMapByField(DishLabelRelEntity.Fields.dishId, dishIds);
        Map<Long, LabelEntity> labelMap = labelService.findMapByIds(dishLabelRelMap.values().stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet()));
        dishLabelRelMap.values().forEach(rel -> {
            LabelEntity labelEntity = labelMap.get(rel.getLabelId());
            if (labelEntity != null) {
                rel.setLabelName(labelEntity.getLabelName());
            }
        });

        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, List<UserDishCollectEntity>> dayGroupMap = collectList.stream().filter(entity -> entity.getCreateTime() != null).collect(Collectors.groupingBy(entity -> dayFormat.format(entity.getCreateTime()), LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        dayGroupMap.forEach((day, records) -> {
            List<Map<String, Object>> dishes = records.stream().map(UserDishCollectEntity::getDishId).distinct().map(dishId -> buildDayGroupDish(dishId, dishMap, dishLabelRelMap, collectTotalNumMap)).filter(Objects::nonNull).toList();
            if (!dishes.isEmpty()) {
                Map<String, Object> dayData = new LinkedHashMap<>();
                dayData.put("day", day);
                dayData.put("dishes", dishes);
                result.add(dayData);
            }
        });

        result.sort(Comparator.comparing((Map<String, Object> item) -> item.get("day").toString()).reversed());
        return ok(result);
    }

    @PostMapping("detail")
    public BaseResponse detail(@RequestBody JSONObject params) {
        //日期，格式为：yyyy-MM-dd
        String date = params.getString("date");
        if (!StringUtils.hasText(date)) {
            throw new ApiException(BaseResponse.Code.fail.code, "日期不能为空");
        }
        try {
            LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ApiException(BaseResponse.Code.fail.code, "date格式必须为yyyy-MM-dd");
        }

        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        if (currentUser == null || !BaseEntity.validId(currentUser.getId())) {
            throw new ApiException(BaseResponse.Code.fail.code, "当前用户未登录");
        }

        List<UserDietRecordEntity> recordList = userDietRecordService.lambdaQuery().eq(UserDietRecordEntity::getUserId, currentUser.getId()).eq(UserDietRecordEntity::getDietDate, date).orderByAsc(UserDietRecordEntity::getDietOrder, UserDietRecordEntity::getId).list();
        if (recordList.isEmpty()) {
            return ok(buildEmptyDietGroups());
        }

        List<Long> dishIds = recordList.stream().map(UserDietRecordEntity::getDishId).filter(Objects::nonNull).distinct().toList();
        Map<Long, DishEntity> dishMap = dishService.findMapByIds(Set.copyOf(dishIds));

        Map<Long, DishLabelRelEntity> dishLabelRelMap = dishLableRelService.findMapByField(DishLabelRelEntity.Fields.dishId, dishIds);
        Map<Long, LabelEntity> labelMap = labelService.findMapByIds(dishLabelRelMap.values().stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet()));
        dishLabelRelMap.values().forEach(rel -> {
            LabelEntity labelEntity = labelMap.get(rel.getLabelId());
            if (labelEntity != null) {
                rel.setLabelName(labelEntity.getLabelName());
            }
        });

        Map<Long, Long> shareCountMap = userShareService.lambdaQuery().in(UserShareEntity::getDishId, dishIds).list().stream().collect(Collectors.groupingBy(UserShareEntity::getDishId, Collectors.counting()));
        Map<Long, Long> collectCountMap = userDishCollectService.lambdaQuery().in(UserDishCollectEntity::getDishId, dishIds).list().stream().collect(Collectors.groupingBy(UserDishCollectEntity::getDishId, Collectors.counting()));

        Map<Integer, List<UserDietRecordEntity>> orderGroupMap = recordList.stream().collect(Collectors.groupingBy(UserDietRecordEntity::getDietOrder));

        List<Map<String, Object>> result = buildEmptyDietGroups();
        result.forEach(group -> {
            Integer dietOrder = (Integer) group.get("order");
            List<Map<String, Object>> dishes = orderGroupMap.getOrDefault(dietOrder, Collections.emptyList()).stream().map(record -> buildDietDish(record, dishMap, dishLabelRelMap, shareCountMap, collectCountMap)).filter(Objects::nonNull).sorted(Comparator.comparing(item -> Long.parseLong(item.get("id").toString()))).toList();
            group.put("dishes", dishes);
            group.remove("order");
        });

        return ok(result);
    }

    @PostMapping("add")
    public BaseResponse add(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        String dietDate = params.getString("dietDate");
        Integer dietOrder = params.getInteger("dietOrder");

        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        Long userId = currentUser.getId();
        validateParams(userId, dishId, dietDate, dietOrder);

        LambdaQueryWrapper<UserDietRecordEntity> wrapper = new LambdaQueryWrapper<UserDietRecordEntity>().eq(UserDietRecordEntity::getUserId, userId).eq(UserDietRecordEntity::getDietDate, dietDate).eq(UserDietRecordEntity::getDietOrder, dietOrder);
        long count = userDietRecordService.count(wrapper);
        if(count >= 3){
            throw new ApiException(BaseResponse.Code.fail.code, "该时间段已添加三道菜，请合理规划饮食");
        }

        LambdaQueryWrapper<UserDietRecordEntity> wrapper1 = wrapper.eq(UserDietRecordEntity::getDishId, dishId);
        long count1 = userDietRecordService.count(wrapper1);
        if(count1 > 0){
            throw new ApiException(BaseResponse.Code.fail.code, "该时间段已添加该菜，请勿重复添加");
        }

        UserDietRecordEntity entity = new UserDietRecordEntity();
        entity.setUserId(userId);
        entity.setDietDate(dietDate);
        entity.setDietOrder(dietOrder);
        entity.setDishId(dishId);
        userDietRecordService.saveOrUpdate(entity);
        return ok(entity);
    }

    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        Long dietId = params.getLong("dietId");
        if (dietId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }
        userDietRecordService.removeById(dietId);
        return ok();
    }

    @RequestMapping("aigc")
    public Flux<String> aigc(/*@RequestBody JSONObject params*/) {
        List<Long> labelIds = labelService.lambdaQuery().in(LabelEntity::getLabelName, Arrays.asList("减脂餐", "月经期")).list().stream().map(BaseEntity::getId).toList();
        /*List<Long> labelIds = params.getList("labelIds", Long.class);*/
        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        Long userId = currentUser.getId();

        String aigcDietJsonLineString;

        try {
            aigcDietJsonLineString = aigcDietJsonLine.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // 1. 查询用户基本信息 (u1)
        UserEntity user = userService.getById(userId);
        JSONObject userInfo = new JSONObject();
        userInfo.put("性别", user.getGender() == 1 ? "男" : "女");
        userInfo.put("年龄", user.getAge() != null ? user.getAge().toString() : "");
        userInfo.put("身高", user.getStature() != null ? user.getStature().toString() : "");
        userInfo.put("体重", user.getWeight() != null ? user.getWeight() + "kg" : "");


        // 2. 查询用户营养目标 (u2)
        List<UserNutritionRelEntity> nutritionRels = userNutritionRelService.lambdaQuery().eq(UserNutritionRelEntity::getUserId, userId).list();
        Set<Long> nutritionIds = nutritionRels.stream().map(UserNutritionRelEntity::getNutritionId).collect(Collectors.toSet());
        Map<Long, NutritionEntity> nutritionMap = nutritionService.findMapByIds(nutritionIds);
        JSONObject nutritionGoals = new JSONObject();
        nutritionRels.forEach(rel -> {
            NutritionEntity nutrition = nutritionMap.get(rel.getNutritionId());
            if (nutrition != null) {
                nutritionGoals.put(nutrition.getName(), rel.getValue());
            }
        });

        // 3. 查询用户饮食历史 (u3)
        List<String> recentThreeDayList = this.recentThreeDays(userId);

        // 4. 查询用户标签/健康状况
        List<LabelEntity> labelEntityList = labelService.listByIds(new HashSet<>(labelIds));
        String labelNames = CollUtil.join(labelEntityList.stream().map(LabelEntity::getLabelName).toList(), ",");
        String knowledgeContext = retrieveNutritionKnowledgeContext(userInfo, nutritionGoals, labelEntityList);

        System.out.println(knowledgeContext);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(aigcDietSystemPrompt);
        Message systemPromptTemplateMessage = systemPromptTemplate.createMessage(Map.of(
                "aigcDietJsonLine", aigcDietJsonLineString,
                "aiFailJson", JSONObject.of("type", "error","message", "错误原因").toJSONString(),
                "knowledgeContext", StringUtils.hasText(knowledgeContext) ? knowledgeContext : "无"
        ));
        PromptTemplate promptTemplate = new PromptTemplate(aigcDietUserPrompt);
        Message message = promptTemplate.createMessage(Map.of("userInfo", userInfo.toJSONString(),"goals", nutritionGoals.toJSONString(),"history", JSONObject.toJSONString(recentThreeDayList),"labels", labelNames));

        Prompt prompt = Prompt.builder().messages(List.of(systemPromptTemplateMessage, message)).build();

        if(true){
            throw new RuntimeException("测试");
        }
        StringBuilder buffer = new StringBuilder();
        StringBuilder fullResponse = new StringBuilder();

        Flux<String> lineFlux = chatModel.stream(prompt.toString()).handle((String chunk, SynchronousSink<String> sink) -> AiResponseUtils.appendAndEmitCompleteLines(buffer, chunk, sink));

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

        return lineFlux.concatWith(remainingFlux).doOnNext(line -> AiResponseUtils.appendLine(fullResponse, line)).doOnComplete(() -> log.info("test41 AI full response:\n{}", fullResponse));

    }

    private String retrieveNutritionKnowledgeContext(JSONObject userInfo, JSONObject nutritionGoals, List<LabelEntity> labelEntityList) {
        try {
            Set<String> labelNames = labelEntityList == null ? Collections.emptySet() : labelEntityList.stream()
                    .map(LabelEntity::getLabelName)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            List<NutritionQueryTask> queryTasks = new ArrayList<>();
            queryTasks.addAll(buildSafetyQueries(labelNames));
            /*queryTasks.addAll(buildGoalQueries(nutritionGoals));*/
            /*queryTasks.addAll(buildProfileQueries(userInfo));*/
            if (queryTasks.isEmpty()) {
                return "";
            }

            List<NutritionHit> mergedHits = queryTasks.parallelStream()
                    .map(this::searchNutrition)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            if (mergedHits.isEmpty()) {
                return "";
            }

            Map<String, NutritionHit> bestHitMap = new LinkedHashMap<>();
            for (NutritionHit hit : mergedHits) {
                hit.setScore(scoreHit(hit, labelNames, nutritionGoals));
                String dedupeKey = hit.getRepositoryId() + "::" + hit.getText();
                NutritionHit existing = bestHitMap.get(dedupeKey);
                if (existing == null || hit.getScore() > existing.getScore()) {
                    bestHitMap.put(dedupeKey, hit);
                }
            }

            List<NutritionHit> selectedHits = bestHitMap.values().stream()
                    .sorted(Comparator.comparingDouble(NutritionHit::getScore).reversed())
                    .limit(NUTRITION_CONTEXT_LIMIT)
                    .toList();
            if (selectedHits.isEmpty()) {
                return "";
            }

            return formatKnowledgeContext(selectedHits);
        } catch (Exception e) {
            log.warn("营养知识检索失败，已降级为普通生成", e);
            return "";
        }
    }

    private List<NutritionHit> searchNutrition(NutritionQueryTask task) {
        try {
            SearchRequest request = SearchRequest.builder().query(task.query()).similarityThreshold(task.threshold()).filterExpression(new FilterExpressionBuilder().eq("type", 2).build()).topK(task.topK()).build();
            List<Document> documents = repositoryVectorStore.similaritySearch(request);
            if (documents == null || documents.isEmpty()) {
                return Collections.emptyList();
            }

            List<NutritionHit> hits = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                String text = normalizeKnowledgeText(document.getText());
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                String repositoryId = Optional.ofNullable(document.getMetadata()).map(metadata -> metadata.get("repository_id")).map(Object::toString).orElse("unknown");
                hits.add(new NutritionHit(task.category(), task.query(), repositoryId, text, i, task.priority()));
            }
            return hits;
        } catch (Exception e) {
            log.warn("营养子查询检索失败，query={}", task.query(), e);
            return Collections.emptyList();
        }
    }

    private List<NutritionQueryTask> buildSafetyQueries(Set<String> labelNames) {
        if (labelNames == null || labelNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<NutritionQueryTask> tasks = new ArrayList<>();
        for (String labelName : labelNames) {
            tasks.add(new NutritionQueryTask("safety", "针对" + labelName + "状态的饮食禁忌与适宜食物建议", 0.90f, 2, 3));
        }
        /*tasks.add(new NutritionQueryTask("safety", "用户当前状态为" + String.join("、", labelNames) + "，今日三餐饮食禁忌与注意事项", 0.68f, 4, 3));*/
        return tasks;
    }

    private List<NutritionQueryTask> buildGoalQueries(JSONObject nutritionGoals) {
        if (nutritionGoals == null || nutritionGoals.isEmpty()) {
            return Collections.emptyList();
        }

        List<NutritionQueryTask> tasks = new ArrayList<>();
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, Object> entry : nutritionGoals.entrySet()) {
            String goalName = entry.getKey();
            String goalValue = entry.getValue() == null ? "" : entry.getValue().toString();
            if (!StringUtils.hasText(goalName) || !StringUtils.hasText(goalValue)) {
                continue;
            }
            summary.add(goalName + goalValue);
            tasks.add(new NutritionQueryTask("goal", goalName + "目标为" + goalValue + "时的饮食建议和食材搭配原则", 0.62f, 3, 2));
        }
        if (!summary.isEmpty()) {
            tasks.add(new NutritionQueryTask("goal", "营养目标为" + String.join("，", summary) + "，请给出三餐营养分配建议", 0.60f, 4, 2));
        }
        return tasks;
    }

    private List<NutritionQueryTask> buildProfileQueries(JSONObject userInfo) {
        String gender = userInfo == null ? "" : userInfo.getString("性别");
        String ageText = userInfo == null ? "" : userInfo.getString("年龄");
        String heightText = userInfo == null ? "" : userInfo.getString("身高");
        String weightText = userInfo == null ? "" : userInfo.getString("体重");

        List<NutritionQueryTask> tasks = new ArrayList<>();
        if (StringUtils.hasText(gender) || StringUtils.hasText(ageText) || (StringUtils.hasText(heightText) && StringUtils.hasText(weightText))) {
            StringBuilder profileQuery = new StringBuilder("用户");
            if (StringUtils.hasText(gender)) {
                profileQuery.append(gender);
            }
            if (StringUtils.hasText(ageText)) {
                profileQuery.append(ageText).append("岁");
            }
            Double bmi = calculateBmi(heightText, weightText);
            if (bmi != null) {
                profileQuery.append("，BMI约").append(String.format(Locale.ROOT, "%.1f", bmi));
            }
            profileQuery.append("，适合的健康饮食建议");
            tasks.add(new NutritionQueryTask("profile", profileQuery.toString(), 0.8f, 2, 1));
        }
        tasks.add(new NutritionQueryTask("profile", "日常三餐营养均衡与健康饮食原则", 0.8f, 2, 1));
        return tasks;
    }

    private double scoreHit(NutritionHit hit, Set<String> labelNames, JSONObject nutritionGoals) {
        double score = hit.getPriority() * 100.0 - hit.getRank() * 5.0;
        if (matchesAnyLabel(hit.getText(), labelNames)) {
            score += 20;
        }
        if (matchesGoalKeywords(hit.getText(), nutritionGoals)) {
            score += 12;
        }
        if (hasContraindicationConflict(hit.getText(), labelNames)) {
            score -= 140;
        }
        if ("safety".equals(hit.getCategory())) {
            score += 8;
        }
        return score;
    }

    private boolean hasContraindicationConflict(String text, Set<String> labelNames) {
        if (!StringUtils.hasText(text) || labelNames == null || labelNames.isEmpty()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        if (labelNames.contains("不吃辣") && containsAny(normalized, List.of("辣椒", "辛辣", "麻辣", "重辣"))) {
            return true;
        }
        if ((labelNames.contains("发烧") || labelNames.contains("感冒"))
                && containsAny(normalized, List.of("生冷", "冷饮", "冰", "辛辣", "油炸", "烧烤", "酒精"))) {
            return true;
        }
        return false;
    }

    private boolean matchesAnyLabel(String text, Set<String> labelNames) {
        if (!StringUtils.hasText(text) || labelNames == null || labelNames.isEmpty()) {
            return false;
        }
        for (String labelName : labelNames) {
            if (StringUtils.hasText(labelName) && text.contains(labelName.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGoalKeywords(String text, JSONObject nutritionGoals) {
        if (!StringUtils.hasText(text) || nutritionGoals == null || nutritionGoals.isEmpty()) {
            return false;
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        for (String goalName : nutritionGoals.keySet()) {
            if (!StringUtils.hasText(goalName)) {
                continue;
            }
            if (goalName.contains("蛋白") && containsAny(normalized, List.of("蛋白", "高蛋白", "优质蛋白"))) {
                return true;
            }
            if (goalName.contains("脂肪") && containsAny(normalized, List.of("低脂", "控脂", "脂肪"))) {
                return true;
            }
            if ((goalName.contains("碳水") || goalName.contains("碳水化合物"))
                    && containsAny(normalized, List.of("碳水", "主食", "全谷物", "杂粮"))) {
                return true;
            }
        }
        return false;
    }

    private String formatKnowledgeContext(List<NutritionHit> selectedHits) {
        Map<String, String> titleMap = Map.of(
                "safety", "【安全禁忌】",
                "goal", "【营养目标】",
                "profile", "【人群补充】"
        );

        List<String> order = List.of("safety", "goal", "profile");
        StringBuilder builder = new StringBuilder();
        int globalNo = 1;
        for (String category : order) {
            List<NutritionHit> categoryHits = selectedHits.stream()
                    .filter(hit -> category.equals(hit.getCategory()))
                    .limit(3)
                    .toList();
            if (categoryHits.isEmpty()) {
                continue;
            }
            builder.append(titleMap.getOrDefault(category, "【补充】")).append('\n');
            for (NutritionHit hit : categoryHits) {
                builder.append(globalNo++).append(". ")
                        .append("(来源").append(hit.getRepositoryId()).append(") ")
                        .append(limitText(hit.getText(), 180))
                        .append('\n');
            }
        }

        return builder.toString().trim();
    }

    private String normalizeKnowledgeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace('\r', '\n').replaceAll("\\n{2,}", "\n").trim();
    }

    private String limitText(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private Double calculateBmi(String heightText, String weightText) {
        Double height = parseNumber(heightText);
        Double weight = parseNumber(weightText);
        if (height == null || weight == null || height <= 0) {
            return null;
        }
        double meter = height > 3 ? height / 100.0 : height;
        if (meter <= 0) {
            return null;
        }
        return weight / (meter * meter);
    }

    private Double parseNumber(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String numeric = text.replaceAll("[^0-9.]", "");
        if (!StringUtils.hasText(numeric)) {
            return null;
        }
        try {
            return Double.parseDouble(numeric);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean containsAny(String source, List<String> keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record NutritionQueryTask(String category, String query, float threshold, int topK, int priority) {
    }

    private static class NutritionHit {
        private final String category;
        private final String query;
        private final String repositoryId;
        private final String text;
        private final int rank;
        private final int priority;
        private double score;

        private NutritionHit(String category, String query, String repositoryId, String text, int rank, int priority) {
            this.category = category;
            this.query = query;
            this.repositoryId = repositoryId;
            this.text = text;
            this.rank = rank;
            this.priority = priority;
        }

        public String getCategory() {
            return category;
        }

        public String getQuery() {
            return query;
        }

        public String getRepositoryId() {
            return repositoryId;
        }

        public String getText() {
            return text;
        }

        public int getRank() {
            return rank;
        }

        public int getPriority() {
            return priority;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }


    public List<String> recentThreeDays(Long userId){
        List<String> dishNames = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate threeDaysAgo = today.minusDays(2);

        List<UserDietRecordEntity> recordList = userDietRecordService.lambdaQuery().eq(UserDietRecordEntity::getUserId, userId).ge(UserDietRecordEntity::getDietDate, threeDaysAgo.format(DATE_FORMATTER)).le(UserDietRecordEntity::getDietDate, today.format(DATE_FORMATTER)).orderByDesc(UserDietRecordEntity::getDietDate).orderByAsc(UserDietRecordEntity::getDietOrder).list();
        if (recordList.isEmpty()) {
            return dishNames;
        }

        List<Long> dishIds = recordList.stream().map(UserDietRecordEntity::getDishId).filter(Objects::nonNull).distinct().toList();
        Map<Long, DishEntity> dishMap = dishService.findMapByIds(Set.copyOf(dishIds));


        for (UserDietRecordEntity userDietRecordEntity : recordList) {
            DishEntity dishEntity = dishMap.get(userDietRecordEntity.getDishId());
            if(dishEntity == null){
                continue;
            }
            dishNames.add(dishEntity.getName());
        }

        return dishNames;
    }

    public List<Map<String, Object>> recentThreeDays2(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate threeDaysAgo = today.minusDays(2);

        List<UserDietRecordEntity> recordList = userDietRecordService.lambdaQuery().eq(UserDietRecordEntity::getUserId, userId).ge(UserDietRecordEntity::getDietDate, threeDaysAgo.format(DATE_FORMATTER)).le(UserDietRecordEntity::getDietDate, today.format(DATE_FORMATTER)).orderByDesc(UserDietRecordEntity::getDietDate).orderByAsc(UserDietRecordEntity::getDietOrder).list();
        if (recordList.isEmpty()) {
            return result;
        }

        List<Long> dishIds = recordList.stream().map(UserDietRecordEntity::getDishId).filter(Objects::nonNull).distinct().toList();
        Map<Long, DishEntity> dishMap = dishService.findMapByIds(Set.copyOf(dishIds));

        Map<Long, DishLabelRelEntity> dishLabelRelMap = dishLableRelService.findMapByField(DishLabelRelEntity.Fields.dishId, dishIds);
        Map<Long, LabelEntity> labelMap = labelService.findMapByIds(dishLabelRelMap.values().stream().map(DishLabelRelEntity::getLabelId).collect(Collectors.toSet()));
        dishLabelRelMap.values().forEach(rel -> {
            LabelEntity labelEntity = labelMap.get(rel.getLabelId());
            if (labelEntity != null) {
                rel.setLabelName(labelEntity.getLabelName());
            }
        });

        Map<Long, Long> shareCountMap = userShareService.lambdaQuery().in(UserShareEntity::getDishId, dishIds).list().stream().collect(Collectors.groupingBy(UserShareEntity::getDishId, Collectors.counting()));
        Map<Long, Long> collectCountMap = userDishCollectService.lambdaQuery().in(UserDishCollectEntity::getDishId, dishIds).list().stream().collect(Collectors.groupingBy(UserDishCollectEntity::getDishId, Collectors.counting()));

        Map<String, List<UserDietRecordEntity>> dateGroupMap = recordList.stream().collect(Collectors.groupingBy(UserDietRecordEntity::getDietDate, LinkedHashMap::new, Collectors.toList()));


        for (int i = 0; i < 3; i++) {
            LocalDate targetDate = today.minusDays(i);
            String dateStr = targetDate.format(DATE_FORMATTER);
            String dayLabel = getDayLabel(targetDate);

            List<UserDietRecordEntity> dayRecords = dateGroupMap.getOrDefault(dateStr, Collections.emptyList());
            Map<Integer, List<UserDietRecordEntity>> orderGroupMap = dayRecords.stream().collect(Collectors.groupingBy(UserDietRecordEntity::getDietOrder));

            List<Map<String, Object>> meals = new ArrayList<>();
            meals.add(createMealGroup(1, "breakfast", "早餐", orderGroupMap, dishMap, dishLabelRelMap, shareCountMap, collectCountMap));
            meals.add(createMealGroup(2, "lunch", "午餐", orderGroupMap, dishMap, dishLabelRelMap, shareCountMap, collectCountMap));
            meals.add(createMealGroup(3, "dinner", "晚餐", orderGroupMap, dishMap, dishLabelRelMap, shareCountMap, collectCountMap));

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", dateStr);
            dayData.put("dayLabel", dayLabel);
            dayData.put("meals", meals);
            result.add(dayData);
        }

        return result;
    }


    private Map<String, Object> createMealGroup(Integer order, String key, String label,
                                                Map<Integer, List<UserDietRecordEntity>> orderGroupMap,
                                                Map<Long, DishEntity> dishMap,
                                                Map<Long, DishLabelRelEntity> dishLabelRelMap,
                                                Map<Long, Long> shareCountMap,
                                                Map<Long, Long> collectCountMap) {
        List<Map<String, Object>> dishes = orderGroupMap.getOrDefault(order, Collections.emptyList()).stream()
                .map(record -> buildDietDish(record, dishMap, dishLabelRelMap, shareCountMap, collectCountMap))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(item -> Long.parseLong(item.get("id").toString())))
                .toList();

        Map<String, Object> meal = new LinkedHashMap<>();
        meal.put("order", order);
        meal.put("key", key);
        meal.put("label", label);
        meal.put("dishes", dishes);
        return meal;
    }

    private List<Map<String, Object>> buildEmptyThreeDaysResult() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            LocalDate targetDate = today.minusDays(i);
            String dateStr = targetDate.format(DATE_FORMATTER);
            String dayLabel = getDayLabel(targetDate);

            List<Map<String, Object>> meals = new ArrayList<>();
            meals.add(createEmptyMealGroup(1, "breakfast", "早餐"));
            meals.add(createEmptyMealGroup(2, "lunch", "午餐"));
            meals.add(createEmptyMealGroup(3, "dinner", "晚餐"));

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", dateStr);
            dayData.put("dayLabel", dayLabel);
            dayData.put("meals", meals);
            result.add(dayData);
        }

        return result;
    }

    private Map<String, Object> createEmptyMealGroup(Integer order, String key, String label) {
        Map<String, Object> meal = new LinkedHashMap<>();
        meal.put("order", order);
        meal.put("key", key);
        meal.put("label", label);
        meal.put("dishes", new ArrayList<>());
        return meal;
    }

    private String getDayLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "今天";
        } else if (date.equals(today.minusDays(1))) {
            return "昨天";
        } else {
            return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE);
        }
    }



    private void validateParams(Long userId, Long dishId, String dietDate, Integer dietOrder) {
        if (userId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "userId不能为空");
        }
        if (dishId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        if (!StringUtils.hasText(dietDate)) {
            throw new ApiException(BaseResponse.Code.fail.code, "dietDate不能为空");
        }
        try {
            LocalDate.parse(dietDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ApiException(BaseResponse.Code.fail.code, "dietDate格式必须为yyyy-MM-dd");
        }
        if (dietOrder == null || dietOrder < 1 || dietOrder > 3) {
            throw new ApiException(BaseResponse.Code.fail.code, "dietOrder仅支持1-3");
        }

        if (userService.getById(userId) == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "用户不存在");
        }
        if (dishService.getById(dishId) == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品不存在");
        }
    }

    private List<Map<String, Object>> buildEmptyDietGroups() {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(createDietGroup(1, "breakfast", "早餐"));
        result.add(createDietGroup(2, "lunch", "午餐"));
        result.add(createDietGroup(3, "dinner", "晚餐"));
        return result;
    }

    private Map<String, Object> createDietGroup(Integer order, String key, String label) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("order", order);
        group.put("key", key);
        group.put("label", label);
        group.put("dishes", new ArrayList<>());
        return group;
    }

    private Map<String, Object> buildDietDish(UserDietRecordEntity record, Map<Long, DishEntity> dishMap, Map<Long, DishLabelRelEntity> dishLabelRelMap, Map<Long, Long> shareCountMap,Map<Long, Long> collectCountMap) {
        DishEntity dishEntity = dishMap.get(record.getDishId());
        if (dishEntity == null) {
            return null;
        }

        List<String> tags = dishLabelRelMap.values().stream().filter(rel -> Objects.equals(rel.getDishId(), record.getDishId())).map(DishLabelRelEntity::getLabelName).filter(StringUtils::hasText).distinct().toList();

        Map<String, Object> dish = new LinkedHashMap<>();
        dish.put("id", record.getId());
        dish.put("dishId", dishEntity.getId());
        dish.put("name", dishEntity.getName());
        dish.put("dishImg", dishEntity.getImgPath());
        dish.put("tags", tags);
        dish.put("shareCount", shareCountMap.getOrDefault(record.getDishId(), 0L));
        dish.put("collectCount", collectCountMap.getOrDefault(record.getDishId(), 0L));
        return dish;
    }

    private Map<String, Object> buildDayGroupDish(Long dishId,
                                                  Map<Long, DishEntity> dishMap,
                                                  Map<Long, DishLabelRelEntity> dishLabelRelMap,
                                                  Map<Long, Long> collectTotalNumMap) {
        DishEntity dishEntity = dishMap.get(dishId);
        if (dishEntity == null) {
            return null;
        }

        String labels = dishLabelRelMap.values().stream()
                .filter(rel -> Objects.equals(rel.getDishId(), dishId))
                .map(DishLabelRelEntity::getLabelName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(","));

        Map<String, Object> dishData = new LinkedHashMap<>();
        dishData.put("id", dishEntity.getId());
        dishData.put("name", dishEntity.getName());
        dishData.put("img", dishEntity.getImgPath());
        dishData.put("labels", labels);
        dishData.put("collectTotalNum", collectTotalNumMap.getOrDefault(dishId, 0L));
        return dishData;
    }
}
