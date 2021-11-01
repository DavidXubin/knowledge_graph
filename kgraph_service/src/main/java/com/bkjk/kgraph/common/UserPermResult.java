package com.bkjk.kgraph.common;
import lombok.Data;

@Data
public class UserPermResult extends CommonResult {
    String token;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(super.toString());
        sb.append(" [");

        if (token != null) {
            sb.append(token);
        }
        sb.append("]");

        return sb.toString();
    }
}
