package com.zicca.zlink.framework.config;

import com.zicca.zlink.framework.aop.TimeCostAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AspectConfig {
    @Bean
    public TimeCostAspect timeCostAspect() {
        return new TimeCostAspect();
    }

}
