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

    private int code = ConstKit.SUCCESS;
    private String msg = ConstKit.SUCCESS_MSG;
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
        this.code = ConstKit.FAIL;
        this.msg = "fail";
        return this;
    }

    public BaseResponse fail(String massage){
        this.code = ConstKit.FAIL;
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
        this.code = ConstKit.FAIL;
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
}


