package com.transferapp.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "services.user-service")
public record UserServiceProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
