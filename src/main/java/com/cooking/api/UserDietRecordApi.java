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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    @PostMapping("aigc")
    public Flux<String> aigc(@RequestBody JSONObject params) {
        List<Long> labelIds = params.getList("labelIds", Long.class);
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

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(aigcDietSystemPrompt);
        Message systemPromptTemplateMessage = systemPromptTemplate.createMessage(Map.of("aigcDietJsonLine", aigcDietJsonLineString, "aiFailJson", JSONObject.of("type", "error","message", "错误原因").toJSONString()));
        PromptTemplate promptTemplate = new PromptTemplate(aigcDietUserPrompt);
        Message message = promptTemplate.createMessage(Map.of("userInfo", userInfo.toJSONString(),"goals", nutritionGoals.toJSONString(),"history", JSONObject.toJSONString(recentThreeDayList),"labels", labelNames));

        Prompt prompt = Prompt.builder().messages(List.of(systemPromptTemplateMessage, message)).build();

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
