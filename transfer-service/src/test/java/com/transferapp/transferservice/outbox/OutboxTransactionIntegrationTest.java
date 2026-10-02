package com.transferapp.transferservice.outbox;

import com.transferapp.transferservice.enums.Currency;
import com.transferapp.transferservice.event.TransferCompletedEvent;
import com.transferapp.transferservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.outbox.publisher-delay-ms=600000")
class OutboxTransactionIntegrationTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rollsBackOutboxRecordWithTheSurroundingTransaction() {
        TransferCompletedEvent event = transferCompletedEvent();

        assertThatThrownBy(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    outboxService.saveTransferCompletedEvent(event);
                    outboxEventRepository.flush();
                    throw new IllegalStateException("Force transaction rollback");
                })
        ).isInstanceOf(IllegalStateException.class)
                .hasMessage("Force transaction rollback");

        assertThat(countByEventId(event.eventId())).isZero();
    }

    private long countByEventId(UUID eventId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_id = ?",
                Long.class,
                eventId
        );
        return count == null ? 0 : count;
    }

    private TransferCompletedEvent transferCompletedEvent() {
        return new TransferCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("25.50"),
                Currency.EUR,
                Instant.parse("2026-09-30T12:00:00Z")
        );
    }
}
