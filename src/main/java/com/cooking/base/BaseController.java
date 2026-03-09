package com.cooking.base;

import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.cooking.core.service.UserService;
import com.cooking.utils.IPAdrressUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Optional;

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
        pageNo = Optional.ofNullable(request.getParameter("pageNo")).map(Integer::valueOf).orElse(0);
        pageSize = Optional.ofNullable(request.getParameter("pageSize")).map(Integer::valueOf).orElse(-1);
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


}
