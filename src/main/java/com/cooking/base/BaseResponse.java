package com.cooking.base;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 返回结果
 * @param
 */
@Slf4j
@Data
public class BaseResponse implements Serializable {

    private int code = Code.success.code;;
    private String msg = Code.success.msg;;
    private Object data;
    private String authCode = "";

    public BaseResponse() {
    }

    public static BaseResponse in(){
        return new BaseResponse();
    }

    public BaseResponse ok(){
        return this;
    }
    public BaseResponse fail(){
        this.code = Code.fail.code;
        this.msg = Code.fail.msg;
        return this;
    }

    public BaseResponse fail(String massage){
        this.code = Code.fail.code;
        this.msg = massage;
        return this;
    }

    public BaseResponse(Object data) {
        super();
        this.data = data;
    }

    public BaseResponse(Throwable e) {
        super();
        this.msg = e.getMessage();
        this.code = Code.fail.code;
    }

    public BaseResponse(int code, String msg, Object data) {
        super();
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public BaseResponse(int code, String msg, Object data, String authCode) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.authCode = authCode;
    }


    public int getCode() {
        return code;
    }

    public BaseResponse setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public BaseResponse setMsg(String msg) {
        this.msg = msg;
        return this;
    }


    public Object getData() {
        return data;
    }

    public BaseResponse setData(Object data) {
        this.data = data;
        return this;
    }

    public String getAuthCode() {
        return authCode;
    }

    public BaseResponse setAuthCode(String authCode) {
        this.authCode = authCode;
        return this;
    }

    public enum Code{

        success(0, "success"),
        fail(1000, "fail"),
        user_wdl(10001, "用户未登录"),
        user_dlsb(10002, "用户登录失败"),

        ;
        public int code;
        public String msg;
        Code(int code, String msg){
            this.code = code;
            this.msg = msg;
        }
    }
}


