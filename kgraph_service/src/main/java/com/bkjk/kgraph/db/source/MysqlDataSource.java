package com.bkjk.kgraph.db.source;

import com.bkjk.kgraph.db.config.CommonConfig;
import com.bkjk.kgraph.db.config.MysqlConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

@Component
public class MysqlDataSource extends HikariDataSource {

    public MysqlDataSource(MysqlConfig mysqlConfig, CommonConfig commonConfig) {
        setDriverClassName(mysqlConfig.getDriver());
        setJdbcUrl(mysqlConfig.getUrl());
        setUsername(mysqlConfig.getUser());
        setPassword(mysqlConfig.getPwd());
        setAutoCommit(mysqlConfig.isAutoCommit());
        setMaximumPoolSize(commonConfig.getMaxTotal());
        setMinimumIdle(commonConfig.getMinIdle());
        setConnectionInitSql(commonConfig.getValidationQuery());
    }
}
