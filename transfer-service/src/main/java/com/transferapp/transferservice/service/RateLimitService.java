package com.transferapp.transferservice.service;


import com.transferapp.transferservice.config.TransferRateLimitProperties;
import com.transferapp.transferservice.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private static final String TRANSFER_RATE_LIMIT_PREFIX =
            "rate-limit:transfers:";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimitScript;
    private final TransferRateLimitProperties properties;

    public void checkTransferLimit(UUID userId) {

        String key = TRANSFER_RATE_LIMIT_PREFIX + userId;

        Long requestCount;

        try {
            requestCount = redisTemplate.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(properties.windowSeconds())
            );
        } catch (DataAccessException exception) {

            log.warn(
                    "Redis unavailable. Skipping transfer rate limit for user {}",
                    userId,
                    exception
            );

            return;
        }

        if (requestCount != null
                && requestCount > properties.maxRequests()) {

            throw new RateLimitExceededException(
                    "Too many transfer requests"
            );
        }
    }
}
