package com.cooking.exceptions;

/**
 * 接口请求异常
 */
public class ApiException extends RuntimeException{
     private int code;

     public ApiException(int code, String message) {
         super(message);
         this.code = code;
     }

    public int getCode() {
        return code;
    }
}
