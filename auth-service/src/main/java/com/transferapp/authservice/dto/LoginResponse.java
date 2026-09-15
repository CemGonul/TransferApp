package com.transferapp.authservice.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
