package com.transferapp.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        Resource privateKey,
        Resource publicKey,
        String issuer,
        String audience,
        Duration expiration
) {
}
