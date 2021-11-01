package com.bkjk.kgraph.config;

import com.bkjk.platform.passport.config.mvc.PassportFeignClientConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 注意引入了Feign Client需要的配置
 *
 * @see PassportFeignClientConfiguration
 */
@Configuration
@Import( {PassportFeignClientConfiguration.class})
public class KgraphWebConfiguration {

}
