package com.transferapp.transferservice.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(

        @NotNull
        UUID sourceAccountId,
        @NotBlank
        String targetAccountNumber,
        @Digits(integer = 17,fraction = 2)
        @NotNull
        BigDecimal amount
) {
}
