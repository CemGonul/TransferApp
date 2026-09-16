package com.transferapp.userservice.dto;


import com.transferapp.userservice.enums.ProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String firstName,
        String lastName,
        String phoneNumber,
        String countryCode,
        ProfileStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
