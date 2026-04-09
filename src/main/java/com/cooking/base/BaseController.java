package com.cooking.base;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.cooking.core.service.UserService;
import com.cooking.utils.IPAdrressUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.BufferedReader;
import java.io.IOException;

@Slf4j
public class BaseController {


    @Value("${upload.url}")
    public String uploadUrl;
    @Value("${upload.path}")
    public String uploadPath;

    protected int pageNo = 0;
    protected int pageSize = -1;


    @Autowired
    private UserService userService;

    @ModelAttribute
    public void init(HttpServletRequest request) {
        log.info("请求接口：{}，请求ip：{}，请求处理类：{}", request.getRequestURI(), IPAdrressUtil.getIpAdrress(request), this.getClass().getCanonicalName());
        pageNo = parseIntParameter(request, "pageNo", 0);
        pageSize = parseIntParameter(request, "pageSize", -1);
        pageNo = pageNo != 0 ? pageNo :  parseIntParameter(request, "pageNum", 0);;
    }




    protected BaseResponse ok() {
        return ok(BaseResponse.Code.success.msg);
    }

    protected BaseResponse ok(String msg) {
        return ok(msg, null);
    }

    protected BaseResponse ok(Object object) {
        return ok(BaseResponse.Code.success.msg, object);
    }

    protected BaseResponse ok(String msg, Object object) {
        return new BaseResponse(BaseResponse.Code.success.code, msg, object);
    }

    protected BaseResponse fail() {
        return fail(BaseResponse.Code.fail.msg);
    }

    protected BaseResponse fail(String msg) {
        return fail(msg, null);
    }

    protected BaseResponse fail(Object object) {
        return fail(BaseResponse.Code.fail.msg, object);
    }

    protected BaseResponse fail(String msg, Object object) {
        return new BaseResponse(BaseResponse.Code.fail.code, msg, object);
    }

    private int parseIntParameter(HttpServletRequest request, String parameterName, int defaultValue) {
        String parameterValue = request.getParameter(parameterName);
        if (StrUtil.isBlank(parameterValue)) {
            parameterValue = readParameterFromJsonBody(request, parameterName);
        }
        if (StrUtil.isBlank(parameterValue)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(parameterValue);
        } catch (NumberFormatException e) {
            log.warn("分页参数解析失败, uri={}, parameterName={}, parameterValue={}", request.getRequestURI(), parameterName, parameterValue);
            return defaultValue;
        }
    }

    private String readParameterFromJsonBody(HttpServletRequest request, String parameterName) {
        String contentType = request.getContentType();
        if (StrUtil.isBlank(contentType) || !StrUtil.containsIgnoreCase(contentType, "application/json")) {
            return null;
        }

        String requestBody = (String) request.getAttribute("cachedRequestBody");
        if (StrUtil.isBlank(requestBody)) {
            requestBody = readRequestBody(request);
            if (StrUtil.isBlank(requestBody)) {
                return null;
            }
            request.setAttribute("cachedRequestBody", requestBody);
        }

        try {
            JSONObject jsonObject = JSONObject.parseObject(requestBody);
            Object value = jsonObject.get(parameterName);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.warn("请求体JSON解析失败, uri={}", request.getRequestURI(), e);
            return null;
        }
    }

    private String readRequestBody(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, len);
            }
            return builder.toString();
        } catch (IOException e) {
            log.warn("读取请求体失败, uri={}", request.getRequestURI(), e);
            return null;
        }
    }

}
