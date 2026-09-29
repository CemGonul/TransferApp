package com.transferapp.transferservice.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix="app.rate-limit.transfer")
@Validated
public record TransferRateLimitProperties(

        @Min(1)
        int maxRequests,

        @Min(1)
        long windowSeconds
) {
}
