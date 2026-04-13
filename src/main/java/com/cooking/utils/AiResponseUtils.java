package com.cooking.utils;

import cn.hutool.json.JSONUtil;
import com.cooking.base.BaseResponse;
import com.cooking.exceptions.ApiException;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;
import reactor.core.publisher.SynchronousSink;

import java.util.List;

public class AiResponseUtils {


    public static String extractJsonText(String aiText) {
        if (!StringUtils.hasText(aiText)) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI返回为空");
        }
        List<String> lines = extractJsonLines(aiText);
        if (lines.isEmpty()) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI返回格式不正确");
        }
        return String.join(System.lineSeparator(), lines);
    }

    public static List<String> extractJsonLines(String aiText) {
        if (!StringUtils.hasText(aiText)) {
            throw new ApiException(BaseResponse.Code.fail.code, "AI返回为空");
        }
        return aiText.lines().map(String::trim).filter(StringUtils::hasText).toList();
    }

    @SafeVarargs
    public static final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
    
    

    public static String extractChunkText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    public static void appendAndEmitCompleteLines(StringBuilder buffer, String chunk, SynchronousSink<String> sink) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        buffer.append(chunk);
        int lineBreakIndex;
        while ((lineBreakIndex = findLineBreak(buffer)) >= 0) {
            String line = buffer.substring(0, lineBreakIndex).trim();
            int deleteLength = lineBreakIndex + 1;
            if (lineBreakIndex + 1 < buffer.length() && buffer.charAt(lineBreakIndex) == '\r' && buffer.charAt(lineBreakIndex + 1) == '\n') {
                deleteLength = lineBreakIndex + 2;
            }
            buffer.delete(0, deleteLength);

            if (!line.isEmpty() && isCompleteJsonObject(line)) {
                sink.next(line);
            }
        }
    }

    public static int findLineBreak(StringBuilder buffer) {
        for (int i = 0; i < buffer.length(); i++) {
            char current = buffer.charAt(i);
            if (current == '\n' || current == '\r') {
                return i;
            }
        }
        return -1;
    }

    public static boolean isCompleteJsonObject(String text) {
        try {
            JSONUtil.parseObj(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void appendLine(StringBuilder builder, String line) {
        if (builder.length() > 0) {
            builder.append(System.lineSeparator());
        }
        builder.append(line);
    }
}
