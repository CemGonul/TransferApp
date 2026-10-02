package com.transferapp.transferservice.outbox;

import com.transferapp.transferservice.entity.OutboxEvent;
import com.transferapp.transferservice.enums.Currency;
import com.transferapp.transferservice.event.TransferCompletedEvent;
import com.transferapp.transferservice.event.TransferEventPublisher;
import com.transferapp.transferservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private TransferEventPublisher transferEventPublisher;

    @Mock
    private OutboxService outboxService;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Test
    void marksEventPublishedOnlyAfterKafkaAcknowledgesSend() {
        UUID outboxEventId = UUID.randomUUID();
        OutboxEvent outboxEvent = outboxEvent(outboxEventId);
        TransferCompletedEvent event = transferCompletedEvent();
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(outboxEvent));
        when(jsonMapper.readValue("event-payload", TransferCompletedEvent.class))
                .thenReturn(event);
        when(transferEventPublisher.publishTransferCompleted(event))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishPendingEvents();

        InOrder inOrder = inOrder(transferEventPublisher, outboxService);
        inOrder.verify(transferEventPublisher).publishTransferCompleted(event);
        inOrder.verify(outboxService).markPublished(outboxEventId);
    }

    @Test
    void leavesEventPendingAndRetriesItOnNextRunWhenKafkaIsUnavailable() {
        UUID outboxEventId = UUID.randomUUID();
        OutboxEvent outboxEvent = outboxEvent(outboxEventId);
        TransferCompletedEvent event = transferCompletedEvent();
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(outboxEvent));
        when(jsonMapper.readValue("event-payload", TransferCompletedEvent.class))
                .thenReturn(event);
        when(transferEventPublisher.publishTransferCompleted(event))
                .thenReturn(
                        CompletableFuture.failedFuture(
                                new IllegalStateException("Kafka unavailable")
                        ),
                        CompletableFuture.completedFuture(null)
                );

        outboxPublisher.publishPendingEvents();

        verify(outboxService, never()).markPublished(outboxEventId);

        outboxPublisher.publishPendingEvents();

        verify(transferEventPublisher, times(2)).publishTransferCompleted(event);
        verify(outboxService).markPublished(outboxEventId);
    }

    @Test
    void leavesEventPendingWhenKafkaSendFailsSynchronously() {
        OutboxEvent outboxEvent = mock(OutboxEvent.class);
        TransferCompletedEvent event = transferCompletedEvent();

        when(outboxEvent.getPayload()).thenReturn("event-payload");
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(outboxEvent));
        when(jsonMapper.readValue("event-payload", TransferCompletedEvent.class))
                .thenReturn(event);
        when(transferEventPublisher.publishTransferCompleted(event))
                .thenThrow(new IllegalStateException("Kafka unavailable"));

        outboxPublisher.publishPendingEvents();

        verify(outboxService, never()).markPublished(any(UUID.class));
    }

    private OutboxEvent outboxEvent(UUID outboxEventId) {
        OutboxEvent outboxEvent = mock(OutboxEvent.class);
        when(outboxEvent.getId()).thenReturn(outboxEventId);
        when(outboxEvent.getPayload()).thenReturn("event-payload");
        return outboxEvent;
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
