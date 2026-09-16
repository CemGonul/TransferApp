package com.transferapp.userservice.dto;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path
) {
}
