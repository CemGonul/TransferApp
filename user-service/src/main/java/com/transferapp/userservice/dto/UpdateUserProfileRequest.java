package com.transferapp.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 32) String phoneNumber,
        @NotBlank @Size(min = 2, max = 2) String countryCode
) {}
