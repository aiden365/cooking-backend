# 计划：优化首页统计接口性能

## 1. 摘要

当前 `IndexApi.statistics` 接口为了获取最近7天的用户、菜谱和用户分享的增长数据，在循环中多次查询数据库，导致性能低下。本计划旨在通过将循环查询重构为单次的分组查询（`GROUP BY`），来显著提升接口性能并降低数据库负载。

## 2. 当前状态分析

`IndexApi.java` 文件中的 `statistics` 方法包含三个独立的for循环，分别用于统计用户、菜谱和分享。每个循环执行7次数据库`count`查询，总计导致`3 * 7 = 21`次数据库查询。这是非常低效的，并且会随着统计维度的增加而变得更糟。

```java
// p:\workspace\projects\cooking\cooking-backend\src\main\java\com\cooking\api\IndexApi.java 中的当前问题代码
for (int i = 6; i >= 0; i--) {
    LocalDateTime day = LocalDate.now().minusDays(i).atStartOfDay();
    long count = userService.lambdaQuery().between(com.cooking.core.entity.UserEntity::getCreateTime, day, day.plusDays(1).minusSeconds(1)).count();
    userLastSevenCount.add(count);
}
```

## 3. 提议的变更

我将采用以下步骤来优化代码，核心思想是“一次查询，内存处理”。

### 3.1. 在 `IndexApi.java` 中创建一个通用的私有辅助方法

为了避免代码重复，我将创建一个名为 `getLastSevenDaysStatistics` 的私有方法。此方法将接受一个通用的MyBatis-Plus服务（`IBaseService`）作为参数，并返回一个包含统计数据的`Map`。

### 3.2. 在辅助方法中实现高效的分组查询

在 `getLastSevenDaysStatistics` 方法内部，我将使用MyBatis-Plus的 `LambdaQueryWrapper` 构建一个查询，该查询：

1. **筛选范围**：只选择 `create_time` 在最近7天内的记录。
2. **自定义查询列**：使用 `.select()` 方法定义要查询的列，包括按天分组的日期 (`DATE(create_time)`) 和对应的计数值 (`count(id)`)。
3. **分组**：使用 `.groupBy()` 按 `DATE(create_time)` 对结果进行分组。

这将把每个统计维度的7次查询减少到只有1次。

### 3.3. 在 `statistics` 主方法中调用辅助方法

我将重构 `statistics` 方法，移除原有的三个for循环，并转而调用新创建的 `getLastSevenDaysStatistics` 辅助方法来获取用户、菜谱和分享的统计数据。

```java
// 伪代码示例
// result.put("userStatistics", getLastSevenDaysStatistics(userService));
// result.put("dishStatistics", getLastSevenDaysStatistics(dishService));
// result.put("useShareStatistics", getLastSevenDaysStatistics(userShareService));
```

## 4. 假设与决策

* **决策**：选择在Controller层(`IndexApi`)内部通过 `LambdaQueryWrapper` 直接实现优化，而不是修改更底层的Service或Mapper层。

* **理由**：这是一个针对特定展示接口的查询优化，将其封装在接口内部可以最大程度地减少对项目其他部分的影响，避免了为该单一功能而修改通用服务和Mapper接口的过度设计。此方法在性能和代码内聚性之间取得了良好平衡。

## 5. 验证步骤

1. **代码审查**：在完成重构后，我会仔细检查 `IndexApi.java` 文件，确保新的实现逻辑正确，并且不再存在循环查询数据库的问题。
2. **功能验证**：手动调用 `GET /index/statistics` 接口，并与重构前（或直接查询数据库）的结果进行比对，确保返回的 `lastSevenCount` 数组以及 `percentRate` 等数据的准确性。

