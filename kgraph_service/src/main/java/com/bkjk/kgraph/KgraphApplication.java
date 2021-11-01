package com.bkjk.kgraph;

import ch.qos.logback.classic.helpers.MDCInsertingServletFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;

//@ServletComponentScan(basePackages="groovy.service")
@SpringBootApplication(exclude = {CassandraAutoConfiguration.class})
public class KgraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(KgraphApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean buildMDCFilter(){
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(new MDCInsertingServletFilter());
        registrationBean.addUrlPatterns("/*");
        //registrationBean.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE - 1);

        return registrationBean;
    }

}
