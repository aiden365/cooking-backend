package com.cooking.exceptions;

/**
 * 其他异常
 */
public class OtherException extends RuntimeException{

    public OtherException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }
}
