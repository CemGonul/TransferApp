package com.transferapp.transferservice.dto;

import com.transferapp.transferservice.enums.Currency;
import com.transferapp.transferservice.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID senderAccountId,
        UUID receiverAccountId,
        BigDecimal amount,
        Currency currency,
        TransferStatus status,
        Instant createdAt,
        Instant completedAt
) {
}
