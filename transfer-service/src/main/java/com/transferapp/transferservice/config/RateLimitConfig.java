package com.transferapp.transferservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RateLimitConfig {

    @Bean
    public RedisScript<Long> rateLimitScript() {
        return RedisScript.of(
                new ClassPathResource("scripts/rate-limit.lua"),
                Long.class
        );
    }
}
