package com.cooking.test;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

public class TestJSqlParser2 {
    public static void main(String[] args) throws Exception {
        String sql = "SELECT * FROM tbl_label WHERE label_name LIKE ? AND type = ? AND exists(select 1 from tbl_dish where id = ?)";
        Statement stmt = CCJSqlParserUtil.parse(sql);

        List<String> paramsInfo = new ArrayList<>();

        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            select.accept(new SelectVisitorAdapter() {
                @Override
                public void visit(PlainSelect plainSelect) {
                    if (plainSelect.getWhere() != null) {
                        plainSelect.getWhere().accept(new ExpressionVisitorAdapter() {
                            private Column currentColumn;

                            @Override
                            protected void visitBinaryExpression(BinaryExpression expr) {
                                if (expr.getLeftExpression() instanceof Column) {
                                    currentColumn = (Column) expr.getLeftExpression();
                                }
                                expr.getLeftExpression().accept(this);
                                if (expr.getRightExpression() instanceof Column) {
                                    currentColumn = (Column) expr.getRightExpression();
                                }
                                expr.getRightExpression().accept(this);
                                currentColumn = null;
                            }

                            @Override
                            public void visit(JdbcParameter parameter) {
                                paramsInfo.add(currentColumn != null ? currentColumn.getColumnName() : "unknown");
                            }
                        });
                    }
                }
            });
        }

        System.out.println("解析到的 ? 对应的列: " + paramsInfo);
    }
}
