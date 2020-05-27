package com.example.tinkerdemo.app;

/**
 * 创建 by hero
 * 时间 2020/5/27
 * 类名
 */
public class ResultBean {

    private int code;
    private String message;
    private boolean success;
    private Object data;


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
