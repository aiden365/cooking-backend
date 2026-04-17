package com.cooking.config;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.cooking.base.IgnoreLogicDelete;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 逻辑删除拦截器
 * 为所有的查询SQL添加逻辑删除条件
 * 除非某个方法被标注为忽略
 *
 * @author chenjc
 * @date 2023/11/20 15:37
 */
@Slf4j
@Component
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class LogicDeleteInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");


        //判断方法是否标记为忽略当前拦截
        String id = mappedStatement.getId();
        String className = id.substring(0, id.lastIndexOf("."));
        String methodName = id.substring(id.lastIndexOf(".") + 1);
        Class<?> maperClass = Class.forName(className);
        for (Method mapperMethod : maperClass.getDeclaredMethods()) {
            //表示当前方法具有忽略标记
            if (mapperMethod.getName().equals(methodName) && mapperMethod.isAnnotationPresent(IgnoreLogicDelete.class)) {
                return invocation.proceed();
            }
        }

        //判断是不是SELECT操作
        if (!SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
            return invocation.proceed();
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        // 执行的SQL语句
        String originalSql = boundSql.getSql();
        /*log.debug("逻辑删除拦截器：原始sql={}", originalSql);*/

        String finalSql = originalSql;
        try {
            finalSql = SqlLogicDeleteHandle.handleSql(SqlLogicDeleteHandle.sanitizeSingleSql(originalSql));
            // 如果 IN () 中没有值，则替换为 IN ('-1')
            finalSql = finalSql.replaceAll("(?i)IN\\s*\\(\\s*\\)", "IN ('-1')");
        } catch (Exception e) {
            finalSql = originalSql;
            log.warn("逻辑删除拦截器，处理SQL失败", e);
            /*throw new RuntimeException("逻辑删除拦截器，处理SQL失败", e);*/
        }

        /*log.debug("逻辑删除拦截器：逻辑删除sql={}", finalSql);*/
        metaStatementHandler.setValue("delegate.boundSql.sql", finalSql);
        return invocation.proceed();
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


    /**
     * 为sql逻辑删除处理类
     *
     * @author chenjc
     * @return
     * @date 2023/11/20 15:18
     */
    public static class SqlLogicDeleteHandle {

        private static final String LOGIC_DELETE_CONDITION = "deleted = 0";

        private static String handleSql(String originalSql) throws JSQLParserException {

            CCJSqlParserManager parserManager = new CCJSqlParserManager();
            Select select = (Select) parserManager.parse(new StringReader(originalSql));

            //单表查询
            if (select instanceof PlainSelect) {

                PlainSelect plainSelect = select.getPlainSelect();
                handlePlainSelect(plainSelect);
            }
            //多表查询
            else if (select instanceof SetOperationList) {
                SetOperationList setOperationList = select.getSetOperationList();
                List<Select> selectBodies = Optional.ofNullable(setOperationList.getSelects()).orElseGet(ArrayList::new);

                for (Select body : selectBodies) {
                    handlePlainSelect(body.getPlainSelect());
                }

            }

            return select.toString();
        }

        public static void handlePlainSelect(PlainSelect plainSelect) {
            //获取查询内容
            FromItem mainFromItem = plainSelect.getFromItem();

            //子查询
            if (mainFromItem instanceof ParenthesedSelect) {

                ParenthesedSelect parenthesedSelect = (ParenthesedSelect) mainFromItem;
                Select selectBody = parenthesedSelect.getSelect();
                if (selectBody instanceof PlainSelect) {
                    //将子查询递归至一般表，然后按一般表办法处理
                    handlePlainSelect((PlainSelect) selectBody);
                }

            }
            //一般表
            else {
                //设置主表的条件
                handleMain(plainSelect);
                //设置查询结果的条件
                handleSelectItem(plainSelect);
                //设置关联表条件
                handleJoin(plainSelect);
                //设置条件
                handleWhere(plainSelect.getWhere());

            }
        }


        @SneakyThrows(Exception.class)
        protected static void handleMain(PlainSelect plainSelect) {

            if (plainSelect.getFromItem() instanceof Table) {

                Table fromItem = (Table) plainSelect.getFromItem();
                // 有别名用别名，无别名用表名，防止字段冲突报错
                Alias fromItemAlias = fromItem.getAlias();
                String originalTableName = fromItem.getName();
                String mainTableName = fromItemAlias == null ? originalTableName : fromItemAlias.getName();


                // 构建子查询 -- 逻辑删除
                String dataSql = mainTableName.concat(".").concat(LOGIC_DELETE_CONDITION);
                if (plainSelect.getWhere() == null) {
                    plainSelect.setWhere(CCJSqlParserUtil.parseCondExpression(dataSql));
                } else {
                    plainSelect.setWhere(new AndExpression(plainSelect.getWhere(), CCJSqlParserUtil.parseCondExpression(dataSql)));
                }
            }
        }

        @SneakyThrows(Exception.class)
        protected static void handleJoin(PlainSelect plainSelect) {

            List<Join> joins = Optional.ofNullable(plainSelect.getJoins()).orElseGet(ArrayList::new);

            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                //关联的是一张表
                if (rightItem instanceof Table) {
                    Table joinTable = (Table) (rightItem);

                    // 有别名用别名，无别名用表名，防止字段冲突报错
                    Alias fromItemAlias = joinTable.getAlias();
                    String originalTableName = joinTable.getName();
                    String mainTableName = fromItemAlias == null ? originalTableName : fromItemAlias.getName();

                    String logicDeleteSql = mainTableName.concat(".").concat(LOGIC_DELETE_CONDITION);

                    LinkedList<Expression> onExpressions = (LinkedList<Expression>) join.getOnExpressions();
                    Expression last = onExpressions.getLast();
                    onExpressions.clear();
                    onExpressions.add(new AndExpression(last, CCJSqlParserUtil.parseCondExpression(logicDeleteSql)));
                }
                //关联的是一个子查询
                else if (rightItem instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) (rightItem);
                    Select select = parenthesedSelect.getSelect();
                    if (select instanceof PlainSelect) {
                        //每一条子查询又是一条新的sql
                        handlePlainSelect((PlainSelect) select);
                    }
                }
            }
        }


        protected static void handleWhere(Expression where) {


            //将not条件转为一般条件处理
            if (where instanceof NotExpression) {
                NotExpression notExpression = (NotExpression) where;
                handleWhere(notExpression.getExpression());
            } else {

                //每条sql的where条件，被分为左右两部分的类似二叉树的结构
                if (where instanceof AndExpression) {
                    AndExpression andExpression = (AndExpression) where;
                    if (andExpression.getLeftExpression() != null) {
                        handleWhere(andExpression.getLeftExpression());
                    }

                    if (andExpression.getRightExpression() != null) {
                        handleWhere(andExpression.getRightExpression());
                    }
                }


                //每个where条件的exists表达式又是一条新的sql
                if (where instanceof ExistsExpression) {
                    ExistsExpression existsExpression = (ExistsExpression) where;
                    handleWhere(existsExpression.getRightExpression());
                }

                //每个where条件的in表达式又是一条新的sql
                if (where instanceof InExpression) {
                    InExpression inExpression = (InExpression) where;
                    Expression expression = inExpression.getRightExpression();

                    if (expression instanceof ParenthesedSelect) {
                        handleWhere(expression);
                    }

                }


                //子查询条件
                if (where instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) where;
                    Select select = parenthesedSelect.getSelect();
                    if (select instanceof PlainSelect) {
                        //每一条子查询又是一条新的sql
                        handlePlainSelect((PlainSelect) select);
                    }

                }
            }


        }

        protected static void handleSelectItem(PlainSelect plainSelect) {

            if (plainSelect == null) {
                return;
            }

            //设置字段子查询条件
            List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
            selectItems.parallelStream().forEach((e) -> {
                if (e.getExpression() instanceof ParenthesedSelect) {
                    ParenthesedSelect parenthesedSelect = (ParenthesedSelect) e.getExpression();
                    Select select = parenthesedSelect.getSelect();
                    if (select instanceof PlainSelect) {
                        //每一条子查询又是一条新的sql
                        handlePlainSelect((PlainSelect) select);
                    }
                }
            });
        }


        public static String sanitizeSingleSql(String sqlStr) {
            final Pattern SQL_DELIMITER_SPLIT =
                    Pattern.compile("((?:'[^']*+'|[^\\n])*+)");
            final StringBuilder builder = new StringBuilder();
            final Matcher matcher = SQL_DELIMITER_SPLIT.matcher(sqlStr);
            while (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (!matcher.group(i).isEmpty()) {
                        builder.append("\n").append(matcher.group(i));
                    }
                }
            }
            return builder.toString();
        }


    }


}
