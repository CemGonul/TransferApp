package com.transferapp.transferservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "idempotency_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "request_hash")
    private String requestHash;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public IdempotencyRequest(
            UUID userId,
            String idempotencyKey,
            String requestHash
    ) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;

        Instant now = Instant.now();

        this.createdAt = now;
        this.expiresAt = now.plus(24, ChronoUnit.HOURS);
    }

    public void attachTransfer(UUID transferId) {
        this.transferId = transferId;
    }
}
