package com.transferapp.transferservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IdempotencyRequestRepositoryTest {

    @Autowired
    private IdempotencyRequestRepository idempotencyRequestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deletesMatchingExpiredRequest() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Instant now = Instant.now();
        insertRequest(
                requestId,
                userId,
                idempotencyKey,
                now.minus(25, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS)
        );

        int deletedRows =
                idempotencyRequestRepository.deleteAllExpiredBefore(now);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(idempotencyRequestRepository.findById(requestId)).isEmpty();
    }

    @Test
    void keepsMatchingActiveRequest() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Instant now = Instant.now();
        insertRequest(
                requestId,
                userId,
                idempotencyKey,
                now.minus(1, ChronoUnit.HOURS),
                now.plus(23, ChronoUnit.HOURS)
        );

        int deletedRows =
                idempotencyRequestRepository.deleteAllExpiredBefore(now);

        assertThat(deletedRows).isZero();
        assertThat(idempotencyRequestRepository.findById(requestId)).isPresent();
    }

    private void insertRequest(
            UUID requestId,
            UUID userId,
            String idempotencyKey,
            Instant createdAt,
            Instant expiresAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO idempotency_requests (
                    id,
                    user_id,
                    idempotency_key,
                    request_hash,
                    created_at,
                    expires_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                requestId,
                userId,
                idempotencyKey,
                "a".repeat(64),
                Timestamp.from(createdAt),
                Timestamp.from(expiresAt)
        );
    }
}
