package com.transferapp.transferservice.outbox;

import com.transferapp.transferservice.entity.OutboxEvent;
import com.transferapp.transferservice.enums.Currency;
import com.transferapp.transferservice.event.TransferCompletedEvent;
import com.transferapp.transferservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void savesTransferCompletedEventAsPendingOutboxRecord() {
        TransferCompletedEvent event = transferCompletedEvent();
        String payload = "{\"eventId\":\"" + event.eventId() + "\"}";
        ArgumentCaptor<OutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        when(jsonMapper.writeValueAsString(event)).thenReturn(payload);

        outboxService.saveTransferCompletedEvent(event);

        verify(outboxEventRepository).save(eventCaptor.capture());

        OutboxEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getEventId()).isEqualTo(event.eventId());
        assertThat(savedEvent.getAggregateId()).isEqualTo(event.transferId());
        assertThat(savedEvent.getEventType()).isEqualTo("TRANSFER_COMPLETED");
        assertThat(savedEvent.getPayload()).isEqualTo(payload);
        assertThat(savedEvent.getCreatedAt()).isNotNull();
        assertThat(savedEvent.getPublishedAt()).isNull();
    }

    @Test
    void marksExistingOutboxEventPublished() {
        UUID outboxEventId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TRANSFER_COMPLETED",
                "event-payload"
        );

        when(outboxEventRepository.findById(outboxEventId))
                .thenReturn(Optional.of(event));

        outboxService.markPublished(outboxEventId);

        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void rejectsMissingOutboxEventWhenMarkingPublished() {
        UUID outboxEventId = UUID.randomUUID();

        when(outboxEventRepository.findById(outboxEventId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> outboxService.markPublished(outboxEventId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Outbox event not found: " + outboxEventId);
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
