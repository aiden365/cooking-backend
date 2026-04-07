package com.cooking.test;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JsonToCsvConverter {

    public static void main(String[] args) {
        File inputTxt = args.length > 0
                ? new File(args[0])
                : new File("E:\\个人资料\\成考\\精简版菜谱.txt");
        File outputCsv = args.length > 1
                ? new File(args[1])
                : new File("E:\\个人资料\\成考\\精简版菜谱3.csv");

        try {
            convertJsonTxtToCsv(inputTxt, outputCsv);
            System.out.println("转换成功，输出文件：" + outputCsv.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("转换失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void convertJsonTxtToCsv(File input, File output) {
        if (!input.exists()) {
            throw new IllegalArgumentException("输入文件不存在：" + input.getAbsolutePath());
        }

        List<JSONObject> jsonRows = new ArrayList<>();
        Set<String> headers = new LinkedHashSet<>();

        try (Reader reader = FileUtil.getReader(input, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = IoUtil.toBuffered(reader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (StrUtil.isBlank(line)) {
                    continue;
                }

                JSONObject jsonObject = JSON.parseObject(line.trim());
                if (jsonObject == null || jsonObject.isEmpty()) {
                    continue;
                }

                jsonRows.add(jsonObject);
                headers.addAll(jsonObject.keySet());
            }
        } catch (Exception e) {
            throw new RuntimeException("读取或解析 txt 文件失败", e);
        }

        if (jsonRows.isEmpty()) {
            System.out.println("txt 文件中没有可转换的数据。");
            return;
        }

        FileUtil.mkParentDirs(output);
        List<String> headerList = new ArrayList<>(headers);

        try (FileOutputStream outputStream = new FileOutputStream(output);
             Writer fileWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CsvWriter writer = CsvUtil.getWriter(fileWriter)) {
            // 给 UTF-8 CSV 增加 BOM，兼容 Excel 打开时的中文编码识别。
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);

            writer.writeHeaderLine(headerList.toArray(new String[0]));

            for (JSONObject jsonRow : jsonRows) {
                List<String> csvRow = new ArrayList<>(headerList.size());
                for (String header : headerList) {
                    Object value = jsonRow.get(header);
                    csvRow.add(value == null ? "" : String.valueOf(value));
                }
                writer.writeLine(csvRow.toArray(new String[0]));
            }
        } catch (Exception e) {
            throw new RuntimeException("写出 csv 文件失败", e);
        }
    }
}
