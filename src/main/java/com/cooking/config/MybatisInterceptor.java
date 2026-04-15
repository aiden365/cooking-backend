package com.cooking.config;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

/**
 * 公共SQL拦截器，对SQL进行一些公共的处理
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class }) })
public class MybatisInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");

        // 判断是不是SELECT操作
        if (!SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
            return invocation.proceed();
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        // 执行的SQL语句
        String originalSql = boundSql.getSql();

        String finalSql = originalSql;
        try {
            // 1. 如果 IN () 中没有值，则替换为 IN ('-1')
            finalSql = finalSql.replaceAll("(?i)IN\\s*\\(\\s*\\)", "IN ('-1')");

            // 2. 解析 SQL，自动为查询、子查询、关联查询添加逻辑删除条件 deleted = 0
            finalSql = addDeletedCondition(finalSql);

        } catch (Exception e) {
            log.error("公共SQL拦截器，处理SQL失败", e);
            // 解析失败时降级使用原始替换后的SQL
        }

        metaStatementHandler.setValue("delegate.boundSql.sql", finalSql);
        return invocation.proceed();
    }

    /**
     * 使用 JSqlParser 解析 SQL 并追加逻辑删除条件
     */
    private String addDeletedCondition(String sql) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(sql);
        if (statement instanceof Select) {
            Select select = (Select) statement;
            processSelectBody(select);
            return select.toString();
        }
        return sql;
    }

    private void processSelectBody(Select selectBody) {
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;

            // 1. 处理主表 (FROM table)
            FromItem fromItem = plainSelect.getFromItem();
            if (fromItem instanceof Table) {
                Table table = (Table) fromItem;
                plainSelect.setWhere(appendDeletedCondition(table, plainSelect.getWhere()));
            } else if (fromItem instanceof ParenthesedSelect) {
                ParenthesedSelect subSelect = (ParenthesedSelect) fromItem;
                if (subSelect.getSelect() != null) {
                    processSelectBody(subSelect.getSelect());
                }
            }

            // 2. 处理 JOIN 表 (JOIN table)
            List<Join> joins = plainSelect.getJoins();
            if (joins != null) {
                for (Join join : joins) {
                    FromItem rightItem = join.getRightItem();
                    if (rightItem instanceof Table) {
                        Table table = (Table) rightItem;
                        join.setOnExpression(appendDeletedCondition(table, join.getOnExpression()));
                    } else if (rightItem instanceof ParenthesedSelect) {
                        ParenthesedSelect subSelect = (ParenthesedSelect) rightItem;
                        if (subSelect.getSelect() != null) {
                            processSelectBody(subSelect.getSelect());
                        }
                    }
                }
            }
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList setOpList = (SetOperationList) selectBody;
            if (setOpList.getSelects() != null) {
                for (Select body : setOpList.getSelects()) {
                    processSelectBody(body);
                }
            }
        }
    }

    private Expression appendDeletedCondition(Table table, Expression expression) {
        // 只处理业务表，避免影响系统表或没有 deleted 字段的表 (假设业务表都是以 tbl_ 开头)
        if (table.getName() != null && !table.getName().toLowerCase().startsWith("tbl_")) {
            return expression;
        }

        // 避免重复添加 deleted 字段的条件
        if (expression != null && expression.toString().toLowerCase().contains("deleted")) {
            return expression;
        }

        String aliasName = table.getAlias() != null ? table.getAlias().getName() : table.getName();
        Column deletedCol = new Column(new Table(aliasName), "deleted");
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(deletedCol);
        equalsTo.setRightExpression(new LongValue(0));

        if (expression == null) {
            return equalsTo;
        } else {
            return new AndExpression(expression, equalsTo);
        }
    }

    /**
     * 生成拦截对象的代理
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    /**
     * mybatis配置的属性
     *
     * @param properties mybatis配置的属性
     */
    @Override
    public void setProperties(Properties properties) {
    }

}
