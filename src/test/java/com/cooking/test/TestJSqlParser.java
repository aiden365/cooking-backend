package com.cooking.test;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

public class TestJSqlParser {
    public static void main(String[] args) throws Exception {
        Statement stmt = CCJSqlParserUtil.parse("SELECT * FROM tbl_user WHERE id = 1");
        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            System.out.println(select.getSelectBody().getClass().getName());
        }
    }
}
