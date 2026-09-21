package com.transferapp.transferservice.dto;

import com.transferapp.transferservice.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        Currency currency,
        BigDecimal balance,
        Instant createdAt
) {
}
