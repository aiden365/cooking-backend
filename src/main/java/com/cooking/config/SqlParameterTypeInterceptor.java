// package com.cooking.config;
//
// import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
// import lombok.extern.slf4j.Slf4j;
// import net.sf.jsqlparser.expression.BinaryExpression;
// import net.sf.jsqlparser.expression.JdbcParameter;
// import net.sf.jsqlparser.expression.operators.relational.*;
// import net.sf.jsqlparser.parser.CCJSqlParserUtil;
// import net.sf.jsqlparser.schema.Column;
// import net.sf.jsqlparser.schema.Table;
// import net.sf.jsqlparser.statement.Statement;
// import net.sf.jsqlparser.statement.select.Join;
// import net.sf.jsqlparser.statement.select.PlainSelect;
// import net.sf.jsqlparser.statement.select.Select;
// import net.sf.jsqlparser.statement.select.SelectVisitor;
// import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
//
// import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
// import net.sf.jsqlparser.util.deparser.SelectDeParser;
// import org.apache.ibatis.executor.parameter.ParameterHandler;
// import org.apache.ibatis.mapping.BoundSql;
// import org.apache.ibatis.mapping.MappedStatement;
// import org.apache.ibatis.mapping.ParameterMapping;
// import org.apache.ibatis.plugin.*;
// import org.apache.ibatis.reflection.MetaObject;
// import org.apache.ibatis.reflection.SystemMetaObject;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
//
// import java.sql.PreparedStatement;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// /**
//  * SQL参数类型强转拦截器
//  * 使用 JSqlParser 解析 SQL 中的 ? 对应的字段，并根据缓存的实体类型进行强转
//  */
// @Slf4j
// @Component
// @Intercepts({
//         @Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})
// })
// public class SqlParameterTypeInterceptor implements Interceptor {
//
//     @Autowired
//     private EntityTypeCache entityTypeCache;
//
//     @Override
//     public Object intercept(Invocation invocation) throws Throwable {
//         ParameterHandler parameterHandler = PluginUtils.realTarget(invocation.getTarget());
//         MetaObject metaParameterHandler = SystemMetaObject.forObject(parameterHandler);
//
//         MappedStatement mappedStatement = (MappedStatement) metaParameterHandler.getValue("mappedStatement");
//         BoundSql boundSql = (BoundSql) metaParameterHandler.getValue("boundSql");
//
//         try {
//             convertParameters(boundSql, mappedStatement);
//         } catch (Exception e) {
//             log.warn("参数类型解析与强转失败, 不影响正常执行: {}", boundSql.getSql(), e);
//         }
//
//         return invocation.proceed();
//     }
//
//     private void convertParameters(BoundSql boundSql, MappedStatement mappedStatement) throws Exception {
//         List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
//         if (parameterMappings == null || parameterMappings.isEmpty()) {
//             return;
//         }
//
//         String sql = boundSql.getSql();
//         Statement statement = CCJSqlParserUtil.parse(sql);
//
//         // 1. 提取表别名映射
//         Map<String, String> tableAliasMap = new HashMap<>();
//         extractTableAliases(statement, tableAliasMap);
//
//         // 2. 追踪所有的 ? 对应的列
//         List<Column> jdbcParameterColumns = new ArrayList<>();
//         trackJdbcParameters(statement, jdbcParameterColumns);
//
//         // 3. 进行类型转换
//         Object parameterObject = boundSql.getParameterObject();
//         if (parameterObject == null) {
//             return;
//         }
//
//         MetaObject metaObject = mappedStatement.getConfiguration().newMetaObject(parameterObject);
//
//         for (int i = 0; i < parameterMappings.size() && i < jdbcParameterColumns.size(); i++) {
//             Column column = jdbcParameterColumns.get(i);
//             if (column == null) continue; // 无法推断的 ?
//
//             String tableName = null;
//             if (column.getTable() != null) {
//                 tableName = column.getTable().getName();
//                 if (tableName == null && column.getTable().getAlias() != null) {
//                     tableName = column.getTable().getAlias().getName();
//                 }
//             }
//
//             // 处理别名，找到真实表名
//             if (tableName != null && tableAliasMap.containsKey(tableName.toLowerCase())) {
//                 tableName = tableAliasMap.get(tableName.toLowerCase());
//             } else if (tableName == null && tableAliasMap.size() == 1) {
//                 // 如果没有指定别名，且查询只涉及一张表，默认使用该表
//                 tableName = tableAliasMap.values().iterator().next();
//             }
//
//             if (tableName == null) continue;
//
//             String columnName = column.getColumnName();
//             Class<?> expectedType = entityTypeCache.getColumnType(tableName, columnName);
//
//             if (expectedType != null) {
//                 ParameterMapping mapping = parameterMappings.get(i);
//                 String propertyName = mapping.getProperty();
//
//                 if (metaObject.hasGetter(propertyName)) {
//                     Object value = metaObject.getValue(propertyName);
//
//                     // 只处理 String 转为 Integer 或 Long
//                     if (value instanceof String) {
//                         String strVal = (String) value;
//                         Object convertedValue = null;
//                         boolean converted = false;
//
//                         try {
//                             if (expectedType == Integer.class || expectedType == int.class) {
//                                 convertedValue = Integer.valueOf(strVal);
//                                 converted = true;
//                             } else if (expectedType == Long.class || expectedType == long.class) {
//                                 convertedValue = Long.valueOf(strVal);
//                                 converted = true;
//                             }
//                         } catch (NumberFormatException ignored) {
//                             // 字符串无法转为数字，忽略
//                         }
//
//                         if (converted) {
//                             log.debug("SQL参数类型强转 [{}.{}] '{}' -> {}", tableName, columnName, strVal, expectedType.getSimpleName());
//                             metaObject.setValue(propertyName, convertedValue);
//                         }
//                     }
//                 }
//             }
//         }
//     }
//
//     private void extractTableAliases(Statement statement, Map<String, String> aliasMap) {
//         if (statement instanceof Select) {
//             Select select = (Select) statement;
//             if (select.getSelectBody() != null) {
//                 net.sf.jsqlparser.statement.select.SelectVisitor<Void> visitor = new SelectVisitorAdapter<Void>() {
//                     @Override
//                     public <S> Void visit(PlainSelect plainSelect, S context) {
//                         if (plainSelect.getFromItem() instanceof Table) {
//                             Table table = (Table) plainSelect.getFromItem();
//                             putAlias(table, aliasMap);
//                         }
//                         if (plainSelect.getJoins() != null) {
//                             for (Join join : plainSelect.getJoins()) {
//                                 if (join.getRightItem() instanceof Table) {
//                                     Table table = (Table) join.getRightItem();
//                                     putAlias(table, aliasMap);
//                                 }
//                             }
//                         }
//                         return null;
//                     }
//                 };
//                 select.getSelectBody().accept(visitor, null);
//             }
//         }
//     }
//
//     private void putAlias(Table table, Map<String, String> aliasMap) {
//         if (table.getAlias() != null) {
//             aliasMap.put(table.getAlias().getName().toLowerCase(), table.getName());
//         } else if (table.getName() != null) {
//             aliasMap.put(table.getName().toLowerCase(), table.getName());
//         }
//     }
//
//     private void trackJdbcParameters(Statement statement, List<Column> jdbcParameterColumns) {
//         StringBuilder buffer = new StringBuilder();
//
//         ExpressionDeParser exprDeParser = new ExpressionDeParser() {
//             private Column currentColumn;
//
//             private void trackBinary(BinaryExpression expr, Runnable superCall) {
//                 Column previous = currentColumn;
//                 if (expr.getLeftExpression() instanceof Column) {
//                     currentColumn = (Column) expr.getLeftExpression();
//                 } else if (expr.getRightExpression() instanceof Column) {
//                     currentColumn = (Column) expr.getRightExpression();
//                 }
//                 superCall.run();
//                 currentColumn = previous;
//             }
//
//             @Override public void visit(EqualsTo expr) { trackBinary(expr, () -> super.visit(expr)); }
//             @Override public void visit(GreaterThan expr) { trackBinary(expr, () -> super.visit(expr)); }
//             @Override public void visit(GreaterThanEquals expr) { trackBinary(expr, () -> super.visit(expr)); }
//             @Override public void visit(MinorThan expr) { trackBinary(expr, () -> super.visit(expr)); }
//             @Override public void visit(MinorThanEquals expr) { trackBinary(expr, () -> super.visit(expr)); }
//             @Override public void visit(NotEqualsTo expr) { trackBinary(expr, () -> super.visit(expr)); }
//             @Override public void visit(LikeExpression expr) { trackBinary(expr, () -> super.visit(expr)); }
//             @Override public void visit(InExpression expr) {
//                 Column previous = currentColumn;
//                 if (expr.getLeftExpression() instanceof Column) {
//                     currentColumn = (Column) expr.getLeftExpression();
//                 }
//                 super.visit(expr);
//                 currentColumn = previous;
//             }
//
//             @Override
//             public void visit(JdbcParameter jdbcParameter) {
//                 jdbcParameterColumns.add(currentColumn);
//                 super.visit(jdbcParameter);
//             }
//         };
//
//         SelectDeParser selectDeParser = new SelectDeParser(exprDeParser, buffer);
//         exprDeParser.setSelectVisitor(selectDeParser);
//         exprDeParser.setBuffer(buffer);
//
//         if (statement instanceof Select) {
//             Select select = (Select) statement;
//             if (select.getSelectBody() != null) {
//                 select.getSelectBody().accept(selectDeParser, null);
//             }
//         }
//     }
//
//     @Override
//     public Object plugin(Object target) {
//         return Plugin.wrap(target, this);
//     }
// }