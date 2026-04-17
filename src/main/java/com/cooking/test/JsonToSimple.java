package com.cooking.test;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JsonToSimple {

    public static void main(String[] args) {
        File inputTxt = args.length > 0
                ? new File(args[0])
                : new File("E:\\个人资料\\成考\\精简版菜谱.txt");
        File outputCsv = args.length > 1
                ? new File(args[1])
                : new File("E:\\个人资料\\成考\\精简版菜谱-simple1.txt");

        try {
            toSimple(inputTxt, outputCsv);
            System.out.println("转换成功，输出文件：" + outputCsv.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("转换失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void toSimple(File input, File output) {
        if (!input.exists()) {
            throw new IllegalArgumentException("输入文件不存在：" + input.getAbsolutePath());
        }

        List<JSONObject> jsonRows = new ArrayList<>();
        Set<String> headers = new LinkedHashSet<>();

        try (Reader reader = FileUtil.getReader(input, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = IoUtil.toBuffered(reader);
             Writer writer = FileUtil.getWriter(output,StandardCharsets.UTF_8,false);
             BufferedWriter bufferedWriter = IoUtil.toBuffered(writer)) {

            String line;
            List<JSONObject> jsonList = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                if (StrUtil.isBlank(line)) {
                    continue;
                }

                JSONObject jsonObject = JSON.parseObject(line.trim());
                if (jsonObject == null || jsonObject.isEmpty()) {
                    continue;
                }
                String dish = jsonObject.getString("dish");
                String tips = jsonObject.getString("description");
                JSONArray recipeInstructions = jsonObject.getJSONArray("recipeInstructions");
                JSONArray recipeIngredient = jsonObject.getJSONArray("recipeIngredient");
                /*JSONObject newDish = new JSONObject();
                newDish.put("菜名",dish);
                newDish.put("食材和调料", recipeIngredient);
                newDish.put("制作步骤", recipeIngredient);
                newDish.put("提示",tips);
                bufferedWriter.write(newDish.toJSONString());
                bufferedWriter.newLine();
                jsonList.add(newDish);*/
                if(StrUtil.containsAnyIgnoreCase(dish, "鲶鱼")){
                    System.out.println();
                }

                String join1 = CollUtil.join(recipeIngredient.toList(String.class).stream().map(StrUtil::cleanBlank).toList(), ",");
                String join2 = CollUtil.join(recipeInstructions.toList(String.class).stream().map(StrUtil::cleanBlank).toList(), ",");
                if(StrUtil.containsAnyIgnoreCase(dish, "Unknown")){
                    continue;
                }
                if(StrUtil.containsAnyIgnoreCase(join1, "Unknown")){
                    continue;
                }
                if(StrUtil.containsAnyIgnoreCase(join2, "Unknown")){
                    continue;
                }


                System.out.println(dish + CollUtil.join(recipeInstructions.toList(String.class), ",") + CollUtil.join(recipeIngredient.toList(String.class), ","));

            }


        } catch (Exception e) {
            throw new RuntimeException("读取或解析 txt 文件失败", e);
        }

        if (jsonRows.isEmpty()) {
            System.out.println("txt 文件中没有可转换的数据。");
            return;
        }

    }
}
