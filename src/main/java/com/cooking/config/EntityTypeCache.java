// package com.cooking.config;
//
// import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
// import com.baomidou.mybatisplus.core.metadata.TableInfo;
// import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.context.event.ApplicationReadyEvent;
// import org.springframework.context.event.EventListener;
// import org.springframework.stereotype.Component;
//
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;
//
// /**
//  * 实体类字段类型缓存
//  * 启动时扫描所有 MyBatis-Plus 的表信息，建立表字段与 Java 类型的映射
//  */
// @Slf4j
// @Component
// public class EntityTypeCache {
//
//     // tableName -> (columnName -> Java Type)
//     private final Map<String, Map<String, Class<?>>> cache = new ConcurrentHashMap<>();
//
//     @EventListener(ApplicationReadyEvent.class)
//     public void init() {
//         log.info("=== 初始化实体类字段类型缓存 ===");
//         List<TableInfo> tableInfos = TableInfoHelper.getTableInfos();
//         if (tableInfos != null) {
//             for (TableInfo tableInfo : tableInfos) {
//                 Map<String, Class<?>> columnMap = new ConcurrentHashMap<>();
//
//                 // 主键
//                 if (tableInfo.getKeyColumn() != null) {
//                     columnMap.put(tableInfo.getKeyColumn().toLowerCase(), tableInfo.getKeyType());
//                 }
//
//                 // 普通字段
//                 for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
//                     columnMap.put(fieldInfo.getColumn().toLowerCase(), fieldInfo.getPropertyType());
//                 }
//
//                 cache.put(tableInfo.getTableName().toLowerCase(), columnMap);
//                 log.debug("缓存表字段类型: {} -> {} 个字段", tableInfo.getTableName(), columnMap.size());
//             }
//         }
//         log.info("=== 实体类字段类型缓存初始化完成，共缓存 {} 个表 ===", cache.size());
//     }
//
//     /**
//      * 获取指定表字段的 Java 类型
//      */
//     public Class<?> getColumnType(String tableName, String columnName) {
//         if (tableName == null || columnName == null) {
//             return null;
//         }
//         Map<String, Class<?>> columnMap = cache.get(tableName.toLowerCase());
//         if (columnMap != null) {
//             return columnMap.get(columnName.toLowerCase());
//         }
//         return null;
//     }
// }