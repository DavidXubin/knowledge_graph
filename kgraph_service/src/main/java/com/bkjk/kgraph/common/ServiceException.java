package com.bkjk.kgraph.common;

public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 5205268290744572556L;

    private int code ;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ServiceException(int code) {
        this.code = code;
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(ReturnCode returnCode, String message) {
        super(returnCode.getMessage() + "[" + message + "]");
        this.code = returnCode.getCode();
    }

    public ServiceException(int code, String message, Throwable cause) {
        super(message + "->" + cause.getMessage(), cause);
        this.code = code;
    }

    public ServiceException(ReturnCode returnCode, Throwable cause) {
        super(returnCode.getMessage() + "->" + cause.getMessage(), cause);
        this.code = returnCode.getCode();
    }

    public ServiceException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }
}