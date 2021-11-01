package com.bkjk.kgraph.common;

/**
 * 系统的返回对象
 *
 * @param <T>
 */
public class AppResult<T> {

    private T result;// 成功时返回的数据

    private int code;//错误编码

    private String msg;// 错误信息


    public static AppResult ok() {
        return ok(null);
    }

    /**
     * 返回一个正确的返回对象
     *
     * @param data
     * @return
     */
    public static AppResult ok(Object result) {
        return new AppResult(result, 200, null);
    }

    /**
     * 返回一个错误的返回对象，并指定错误码和错误信息
     *
     * @param code
     * @param msg
     * @return
     */
    public static AppResult error(int code, String msg) {
        return new AppResult(null, code, msg);
    }

    public AppResult() {
        super();
        this.code = 200;
    }

    public AppResult(T result, int errCode, String errMsg) {
        super();
        //this.success = success;
        this.result = result;
        this.msg = errMsg;
        this.code = errCode;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "AppResult{" + " result=" + result + ", code=" + code + ", msg='" + msg + '\'' + '}';
    }
}