package com.bkjk.kgraph.permission;

public class MetaResult {

    private int error;
    private String message;
    private SqlInfo sqlinfo;

    public MetaResult(int error, String message) {
        this.error = error;
        this.message = message;
        this.sqlinfo = new SqlInfo();
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SqlInfo getSqlinfo() {
        return sqlinfo;
    }

    public void setSqlinfo(SqlInfo sqlinfo) {
        this.sqlinfo = sqlinfo;
    }
}
