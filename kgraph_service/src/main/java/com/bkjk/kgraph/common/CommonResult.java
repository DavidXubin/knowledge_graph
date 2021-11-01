package com.bkjk.kgraph.common;
import lombok.Data;

@Data
public class CommonResult {
    protected int error;

    protected String message;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(" [");
        sb.append("error=").append(error);
        sb.append(", message=").append(message);
        sb.append("]");
        return sb.toString();
    }
}
