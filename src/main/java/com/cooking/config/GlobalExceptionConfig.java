package com.cooking.config;

import com.cooking.base.BaseResponse;
import com.cooking.exceptions.OtherException;
import com.cooking.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionConfig {

    @ExceptionHandler(value = Exception.class)
    public BaseResponse exceptionHandler(HttpServletRequest req, Exception e){

        if (e instanceof OtherException){
            return BaseResponse.in().fail(e.getMessage());
        }
        else if(e instanceof ApiException){
            return BaseResponse.in().fail(e.getMessage()).setCode(((ApiException) e).getCode());
        }
        log.error("未知异常，原因是：",e);
        return new BaseResponse(e);
    }
}