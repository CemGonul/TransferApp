package com.transferapp.transferservice.outbox;

import com.transferapp.transferservice.entity.OutboxEvent;
import com.transferapp.transferservice.enums.Currency;
import com.transferapp.transferservice.event.TransferCompletedEvent;
import com.transferapp.transferservice.event.TransferEventPublisher;
import com.transferapp.transferservice.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherKafkaUnavailableTest {

    @Test
    void leavesOutboxEventPendingWhenKafkaBrokerCannotBeReached() {
        OutboxEventRepository outboxEventRepository =
                mock(OutboxEventRepository.class);
        OutboxService outboxService = mock(OutboxService.class);
        JsonMapper jsonMapper = mock(JsonMapper.class);
        OutboxEvent outboxEvent = mock(OutboxEvent.class);
        TransferCompletedEvent event = transferCompletedEvent();
        UUID outboxEventId = UUID.randomUUID();

        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(outboxEvent));
        when(outboxEvent.getId()).thenReturn(outboxEventId);
        when(outboxEvent.getEventId()).thenReturn(event.eventId());
        when(outboxEvent.getPayload()).thenReturn("event-payload");
        when(jsonMapper.readValue("event-payload", TransferCompletedEvent.class))
                .thenReturn(event);

        DefaultKafkaProducerFactory<String, TransferCompletedEvent> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProperties());
        KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate =
                new KafkaTemplate<>(producerFactory);
        TransferEventPublisher transferEventPublisher =
                new TransferEventPublisher(kafkaTemplate);
        OutboxPublisher outboxPublisher = new OutboxPublisher(
                outboxEventRepository,
                transferEventPublisher,
                outboxService,
                jsonMapper
        );

        try {
            assertThatCode(outboxPublisher::publishPendingEvents)
                    .doesNotThrowAnyException();

            verify(outboxService, never()).markPublished(outboxEventId);
        } finally {
            producerFactory.destroy();
        }
    }

    private Map<String, Object> producerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "127.0.0.1:1"
        );
        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JacksonJsonSerializer.class
        );
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 500);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 500);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 1000);
        properties.put(ProducerConfig.RETRIES_CONFIG, 0);
        return properties;
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
