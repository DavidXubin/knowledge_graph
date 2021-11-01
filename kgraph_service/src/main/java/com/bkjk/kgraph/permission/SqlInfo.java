package com.bkjk.kgraph.permission;

public class SqlInfo {
    private Integer complexity;

    private Boolean crossjoin;

    private String sqltype;

    public Integer getComplexity() {
        return complexity;
    }

    public void setComplexity(Integer complexity) {
        this.complexity = complexity;
    }

    public Boolean getCrossjoin() {
        return crossjoin;
    }

    public void setCrossjoin(Boolean crossjoin) {
        this.crossjoin = crossjoin;
    }

    public String getSqltype() {
        return sqltype;
    }

    public void setSqltype(String sqltype) {
        this.sqltype = sqltype;
    }
}
