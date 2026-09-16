package com.transferapp.userservice.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix="security.jwt")
public record JwtProperties(
        Resource publicKey,
        String issuer,
        String audience
) {
}
