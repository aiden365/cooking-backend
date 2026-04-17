package com.cooking.test;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.CharsetUtil;
import com.alibaba.fastjson.JSONArray;

import java.util.List;

import java.io.File;


public class CsvToTxt {

    public static void readCsvAndPrint(String csvFilePath) {
        // 创建CSV读取器，设置第一行为表头
        CsvReader reader = CsvUtil.getReader();
        reader.setContainsHeader(true); // 启用表头别名

        // 读取CSV文件
        List<CsvRow> rows = reader.read(FileUtil.file(csvFilePath), CharsetUtil.charset("gbk")).getRows();

        // 遍历每一行并输出
        for (CsvRow row : rows) {
            // 方式1：直接输出整行
            //System.out.println(row.getRawList());

            // 方式2：按字段名访问（如果有表头）
            String name = row.getByName("name");
            String description = row.getByName("description");
            String recipeIngredient = CollUtil.join(JSONArray.parseArray(row.getByName("recipeIngredient")).toJavaList(String.class), ",");
            String recipeInstructions = CollUtil.join(JSONArray.parseArray(row.getByName("recipeInstructions")).toJavaList(String.class), ",");
            System.out.println("菜名：" + name + ", 食材：" + recipeIngredient + "制作步骤：" + recipeInstructions + ", 描述：" + description);
        }
    }

    public static void main(String[] args) {
        readCsvAndPrint("E:\\个人资料\\成考\\精简版菜谱3.csv");
    }




}
