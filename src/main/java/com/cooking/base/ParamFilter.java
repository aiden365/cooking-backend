package com.cooking.base;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//@WebFilter
public class ParamFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            PageParameterWrapper wrapper = new PageParameterWrapper((HttpServletRequest) request);
            wrapper.addDefaultPageParameters();
            // 将包装后的请求传给后续的 Filter 或 Controller
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    public static class PageParameterWrapper extends HttpServletRequestWrapper {

        private final Map<String, String[]> modifiableParameters;

        public PageParameterWrapper(HttpServletRequest request) {
            super(request);
            // 复制一份原始参数，以便修改
            this.modifiableParameters = new HashMap<>(request.getParameterMap());
        }

        public void addDefaultPageParameters() {
            // 如果不存在 pageNo，设为 "0"
            if (!modifiableParameters.containsKey("pageNo")) {
                modifiableParameters.put("pageNo", new String[]{"0"});
            }
            // 如果不存在 pageSize，设为 "-1"
            if (!modifiableParameters.containsKey("pageSize")) {
                modifiableParameters.put("pageSize", new String[]{"-1"});
            }
        }

        @Override
        public String getParameter(String name) {
            String[] values = modifiableParameters.get(name);
            return (values != null && values.length > 0) ? values[0] : null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return modifiableParameters;
        }

        @Override
        public String[] getParameterValues(String name) {
            return modifiableParameters.get(name);
        }
    }
}
