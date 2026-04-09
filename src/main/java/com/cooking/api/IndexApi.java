package com.cooking.api;

import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.base.BaseEntity;
import com.cooking.core.entity.DishAppraisesEntity;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.entity.UserShareEntity;
import com.cooking.core.service.DishAppraisesService;
import com.cooking.core.service.DishService;
import com.cooking.core.service.UserShareService;
import com.cooking.core.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("index")
public class IndexApi extends BaseController {

    @Resource
    private UserService userService;

    @Resource
    private DishService dishService;

    @Resource
    private UserShareService userShareService;

    @Resource
    private DishAppraisesService dishAppraisesService;

    @PostMapping("statistics")
    public BaseResponse statistics(@RequestBody(required = false) Object params) {
        Map<String, Object> result = new HashMap<>();
        result.put("userStatistics", buildStatistics("userTotalCount", userService.lambdaQuery().count(), queryLastSevenDays(UserEntity.class)));
        result.put("dishStatistics", buildStatistics("dishTotalCount", dishService.lambdaQuery().count(), queryLastSevenDays(DishEntity.class)));
        result.put("useShareStatistics", buildStatistics("shareTotalCount", userShareService.lambdaQuery().count(), queryLastSevenDays(UserShareEntity.class)));
        result.put("dishTotalScore", buildDishTotalScore());
        return ok(result);
    }

    @PostMapping("dishCheckData")
    public BaseResponse dishCheckData(@RequestBody(required = false) Object params) {
        LocalDate today = LocalDate.now();
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate lastWeekStart = currentWeekStart.minusWeeks(1);

        Map<String, Object> result = new HashMap<>();
        result.put("currentWeekData", buildDishCheckWeekData(currentWeekStart));
        result.put("lastWeekData", buildDishCheckWeekData(lastWeekStart));
        return ok(result);
    }

    @PostMapping("dishDynamicsData")
    public BaseResponse dishDynamicsData(@RequestBody(required = false) Object params) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        Date startTime = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endTime = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant());

        List<DishEntity> aigcDishList = dishService.lambdaQuery()
                .eq(DishEntity::getSourceType, 2)
                .between(BaseEntity::getCreateTime, startTime, endTime)
                .list();
        List<DishEntity> checkDishList = dishService.lambdaQuery()
                .eq(DishEntity::getCheckStatus, 2)
                .between(DishEntity::getCheckTime, startTime, endTime)
                .list();

        Map<LocalDate, Long> aigcCountMap = buildDishCountMap(aigcDishList, false);
        Map<LocalDate, Long> checkCountMap = buildDishCountMap(checkDishList, true);

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate currentDate = today.minusDays(i);
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("day", "%d年%d月%d日".formatted(currentDate.getYear(), currentDate.getMonthValue(), currentDate.getDayOfMonth()));
            dayData.put("aigcCount", aigcCountMap.getOrDefault(currentDate, 0L));
            dayData.put("checkCount", checkCountMap.getOrDefault(currentDate, 0L));
            result.add(dayData);
        }
        return ok(result);
    }

    private Map<String, Object> buildStatistics(String totalFieldName, Long totalCount, List<Long> lastSevenCount) {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put(totalFieldName, totalCount == null ? 0L : totalCount);
        statistics.put("lastSevenCount", lastSevenCount);
        statistics.put("percentRate", buildPercentRate(lastSevenCount));
        return statistics;
    }

    private List<Long> queryLastSevenDays(Class<?> entityClass) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        Date startTime = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endTime = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant());

        List<? extends BaseEntity> entityList;
        if (entityClass == UserEntity.class) {
            entityList = userService.lambdaQuery().between(BaseEntity::getCreateTime, startTime, endTime).list();
        } else if (entityClass == DishEntity.class) {
            entityList = dishService.lambdaQuery().between(BaseEntity::getCreateTime, startTime, endTime).list();
        } else {
            entityList = userShareService.lambdaQuery().between(BaseEntity::getCreateTime, startTime, endTime).list();
        }

        Map<LocalDate, Long> countMap = new HashMap<>();
        for (BaseEntity entity : entityList) {
            if (entity.getCreateTime() == null) {
                continue;
            }
            LocalDate createDate = entity.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            countMap.put(createDate, countMap.getOrDefault(createDate, 0L) + 1);
        }

        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            counts.add(countMap.getOrDefault(currentDate, 0L));
        }
        return counts;
    }

    private String buildPercentRate(List<Long> lastSevenCount) {
        if (lastSevenCount == null || lastSevenCount.size() < 2) {
            return "0%";
        }
        long todayCount = lastSevenCount.get(lastSevenCount.size() - 1);
        long yesterdayCount = lastSevenCount.get(lastSevenCount.size() - 2);
        if (yesterdayCount == 0) {
            return todayCount == 0 ? "0%" : "100%";
        }
        BigDecimal percent = BigDecimal.valueOf(todayCount - yesterdayCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(yesterdayCount), 2, RoundingMode.HALF_UP);
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

    private Map<String, Object> buildDishCheckWeekData(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        Date createStartTime = Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date createEndTime = Date.from(weekEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant());

        List<DishEntity> aigcDishList = dishService.lambdaQuery()
                .eq(DishEntity::getSourceType, 2)
                .between(BaseEntity::getCreateTime, createStartTime, createEndTime)
                .list();

        List<DishEntity> checkDishList = dishService.lambdaQuery()
                .eq(DishEntity::getCheckStatus, 2)
                .between(DishEntity::getCheckTime, createStartTime, createEndTime)
                .list();

        Map<String, Object> weekData = new HashMap<>();
        weekData.put("aigcDishCount", buildDailyCount(weekStart, aigcDishList, false));
        weekData.put("checkDishCount", buildDailyCount(weekStart, checkDishList, true));
        return weekData;
    }

    private List<Long> buildDailyCount(LocalDate startDate, List<DishEntity> dishList, boolean useCheckTime) {
        Map<LocalDate, Long> countMap = buildDishCountMap(dishList, useCheckTime);

        List<Long> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            result.add(countMap.getOrDefault(startDate.plusDays(i), 0L));
        }
        return result;
    }

    private Map<LocalDate, Long> buildDishCountMap(List<DishEntity> dishList, boolean useCheckTime) {
        Map<LocalDate, Long> countMap = new HashMap<>();
        for (DishEntity dishEntity : dishList) {
            Date targetDate = useCheckTime ? dishEntity.getCheckTime() : dishEntity.getCreateTime();
            if (targetDate == null) {
                continue;
            }
            LocalDate localDate = targetDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            countMap.put(localDate, countMap.getOrDefault(localDate, 0L) + 1);
        }
        return countMap;
    }

    private String buildDishTotalScore() {
        List<DishAppraisesEntity> appraiseList = dishAppraisesService.lambdaQuery().list();
        if (appraiseList == null || appraiseList.isEmpty()) {
            return "0%";
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        for (DishAppraisesEntity entity : appraiseList) {
            BigDecimal manipulationScore = BigDecimal.valueOf(defaultScore(entity.getManipulationScore())).multiply(BigDecimal.valueOf(0.3));
            BigDecimal equalScore = BigDecimal.valueOf(defaultScore(entity.getEqualScore())).multiply(BigDecimal.valueOf(0.3));
            BigDecimal satisfactionScore = BigDecimal.valueOf(defaultScore(entity.getSatisfactionScore())).multiply(BigDecimal.valueOf(0.4));
            totalScore = totalScore.add(manipulationScore).add(equalScore).add(satisfactionScore);
        }

        BigDecimal averageScore = totalScore.divide(BigDecimal.valueOf(appraiseList.size()), 2, RoundingMode.HALF_UP);
        BigDecimal percentScore = averageScore.multiply(BigDecimal.TEN);
        return percentScore.stripTrailingZeros().toPlainString() + "%";
    }

    private int defaultScore(Integer score) {
        return score == null ? 0 : score;
    }

}
