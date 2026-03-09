package com.cooking.config;

import com.cooking.base.BaseResponse;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.UserService;
import com.cooking.exceptions.ApiException;
import com.cooking.utils.SystemContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserService userService;


    /**
     * 请求方法执行之前
     * 返回true则通过
     *
     * @param request
     * @param response
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        StringBuffer requestURL = request.getRequestURL();
        log.info("preHandle请求URL：" + requestURL.toString());
        String token = request.getParameter("token");
        Object object = stringRedisTemplate.opsForValue().get(token);
        if(object == null){
            throw new ApiException(BaseResponse.Code.user_wdl.code, "用户未登录");
        }
        try{
            UserEntity user = userService.getById((Long) object);
            SystemContextHelper.setUser(user);
        }catch (Exception e){
            throw new ApiException(BaseResponse.Code.user_dlsb.code, "用户登录失败");
        }
        return true;
    }

    /**
     * 返回modelAndView之前执行
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        log.info("postHandle返回modelAndView之前");
    }

    /**
     * 执行Handler完成执行此方法
     * @param request
     * @param response
     * @param handler
     * @param ex
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.info("afterCompletion执行完请求方法完全返回之后");
    }
}
