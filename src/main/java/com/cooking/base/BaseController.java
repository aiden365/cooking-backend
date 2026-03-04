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

@Slf4j
public class BaseController {


    @Value("${upload.url}")
    public String uploadUrl;
    @Value("${upload.path}")
    public String uploadPath;


    //当前访问授权信息
    protected static final ThreadLocal<AuthInfo> authInfo = new TransmittableThreadLocal<>();
    //当前访问授权码
    protected static final ThreadLocal<String> authCode = new TransmittableThreadLocal<>();

    @Autowired
    private UserService userService;

    @ModelAttribute
    public void init(HttpServletRequest request) {
        log.info("请求接口：{}，请求ip：{}，请求处理类：{}", request.getRequestURI(), IPAdrressUtil.getIpAdrress(request), this.getClass().getCanonicalName());
        authCode.set(request.getHeader(ConstKit.HEADER_AUTH_CODE));
        authInfo.set(getAuthInfo());
    }

    /**
     * 当前授权设备信息
     *
     * @return
     */
    public AuthInfo getAuthInfo() {

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        if (StrUtil.isEmpty(authCode.get())) {
            return null;
        }
        return new AuthInfo().setUserId("1");
    }


    /**
     * 更新当前授权设备信息
     *
     * @return
     */
    public synchronized String updateAuthInfo(Object authInfo) {
        return authCode.get();
    }


    protected BaseResponse ok() {
        return ok(ConstKit.SUCCESS_MSG);
    }

    protected BaseResponse ok(String msg) {
        return ok(msg, null);
    }

    protected BaseResponse ok(Object object) {
        return ok(ConstKit.SUCCESS_MSG, object);
    }

    protected BaseResponse ok(String msg, Object object) {
        return new BaseResponse(ConstKit.SUCCESS, msg, object, authCode.get());
    }

    protected BaseResponse fail() {
        return fail(ConstKit.FAIL_MSG);
    }

    protected BaseResponse fail(String msg) {
        return fail(msg, null);
    }

    protected BaseResponse fail(Object object) {
        return fail(ConstKit.FAIL_MSG, object);
    }

    protected BaseResponse fail(String msg, Object object) {
        return new BaseResponse(ConstKit.FAIL, msg, object, authCode.get());
    }


}
