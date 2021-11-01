package com.bkjk.kgraph.common;

public enum ReturnCode {
    SERVICE_ERROR(30000,"发生未知错误!"),
    SERVICE_JSON_PARSE_ERROR(30001, "json解析错误"),
    SERVICE_PLUGIN_ERROR(30002,"运行groovy插件错误!"),
    SERVICE_PERMISSION_ERROR(30003,"获取权限错误!"),
    SERVICE_REDIS_ERROR(30004,"访问redis错误!"),
    SERVICE_PARAM_ERROR(30005,"参数错误或不全!")
    ;

    private int code;
    private String message;
    ReturnCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
