package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishLabelRelEntity;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.LabelEntity;
import com.cooking.core.entity.UserDishCollectEntity;
import com.cooking.core.entity.UserDietRecordEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.entity.UserShareEntity;
import com.cooking.core.service.DishLableRelService;
import com.cooking.core.service.DishService;
import com.cooking.core.service.LabelService;
import com.cooking.core.service.UserDishCollectService;
import com.cooking.core.service.UserDietRecordService;
import com.cooking.core.service.UserShareService;
import com.cooking.core.service.UserService;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.SystemContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * User diet record controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
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

    @PostMapping("save")
    public BaseResponse save(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        Long dishId = params.getLong("dishId");
        String dietDate = params.getString("dietDate");
        Integer dietOrder = params.getInteger("dietOrder");

        validateParams(userId, dishId, dietDate, dietOrder);

        LambdaQueryWrapper<UserDietRecordEntity> wrapper = new LambdaQueryWrapper<UserDietRecordEntity>()
                .eq(UserDietRecordEntity::getUserId, userId)
                .eq(UserDietRecordEntity::getDietDate, dietDate)
                .eq(UserDietRecordEntity::getDietOrder, dietOrder);
        UserDietRecordEntity entity = userDietRecordService.getOne(wrapper);
        if (entity == null) {
            entity = new UserDietRecordEntity();
            entity.setUserId(userId);
            entity.setDietDate(dietDate);
            entity.setDietOrder(dietOrder);
        }
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
}
