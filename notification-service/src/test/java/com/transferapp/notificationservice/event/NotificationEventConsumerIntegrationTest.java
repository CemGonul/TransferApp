package com.transferapp.notificationservice.event;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
class NotificationEventConsumerIntegrationTest {

    @MockitoSpyBean
    private NotificationEventConsumer notificationEventConsumer;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Test
    void consumesAndDeserializesTransferCompletedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "transferId": "%s",
                  "senderUserId": "%s",
                  "receiverUserId": "%s",
                  "amount": 25.50,
                  "currency": "EUR",
                  "completedAt": "2026-09-29T12:00:00Z"
                }
                """.formatted(
                eventId,
                transferId,
                senderUserId,
                receiverUserId
        );

        waitForListenerAssignment();

        try (KafkaProducer<String, String> producer = createProducer()) {
            producer.send(
                    new ProducerRecord<>(
                            "transfer-completed",
                            "malformed-event",
                            "{not-valid-json"
                    )
            ).get();

            producer.send(
                    new ProducerRecord<>(
                            "transfer-completed",
                            transferId.toString(),
                            payload
                    )
            ).get();
        }

        verify(notificationEventConsumer, timeout(10_000)).consume(
                argThat(event ->
                        event.eventId().equals(eventId)
                                && event.transferId().equals(transferId)
                                && event.senderUserId().equals(senderUserId)
                                && event.receiverUserId().equals(receiverUserId)
                                && event.currency().name().equals("EUR")
                                && event.amount().toPlainString().equals("25.50")
                                && event.completedAt().equals(
                                        Instant.parse("2026-09-29T12:00:00Z")
                                )
                )
        );
    }

    private void waitForListenerAssignment() throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(10);

        while (!hasAssignedPartitions() && Instant.now().isBefore(deadline)) {
            Thread.sleep(100);
        }

        assertThat(hasAssignedPartitions()).isTrue();
    }

    private boolean hasAssignedPartitions() {
        return kafkaListenerEndpointRegistry
                .getListenerContainers()
                .stream()
                .anyMatch(container ->
                        container.getAssignedPartitions() != null
                                && !container.getAssignedPartitions().isEmpty()
                );
    }

    private KafkaProducer<String, String> createProducer() {
        return new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        ));
    }
}
