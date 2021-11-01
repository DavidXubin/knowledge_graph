package com.bkjk.kgraph.web;

import com.bkjk.platform.passport.config.mvc.UcPassportAuthenticationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
public class MvcConfig extends UcPassportAuthenticationConfiguration {

    @Bean
    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration addInterceptor = registry.addInterceptor(loginInterceptor());
        addInterceptor.excludePathPatterns("/**");

        //addInterceptor.addPathPatterns("/**");
    }

}
