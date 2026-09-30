package com.transferapp.transferservice.event;

import com.transferapp.transferservice.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        UUID transferId,
        UUID senderUserId,
        UUID receiverUserId,
        BigDecimal amount,
        Currency currency,
        Instant completedAt
) {
}