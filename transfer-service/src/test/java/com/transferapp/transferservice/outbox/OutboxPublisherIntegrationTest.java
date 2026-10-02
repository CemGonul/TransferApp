package com.transferapp.transferservice.outbox;

import com.transferapp.transferservice.entity.OutboxEvent;
import com.transferapp.transferservice.enums.Currency;
import com.transferapp.transferservice.event.TransferCompletedEvent;
import com.transferapp.transferservice.repository.OutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.transferapp.transferservice.config.KafkaTopicConfig.TRANSFER_COMPLETED_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.outbox.publisher-delay-ms=600000")
class OutboxPublisherIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void publishesPendingOutboxEventAndMarksItPublishedWhenKafkaIsAvailable() {
        TransferCompletedEvent event = transferCompletedEvent();
        OutboxEvent outboxEvent = new OutboxEvent(
                event.eventId(),
                event.transferId(),
                "TRANSFER_COMPLETED",
                jsonMapper.writeValueAsString(event)
        );

        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            TopicPartition topicPartition =
                    new TopicPartition(TRANSFER_COMPLETED_TOPIC, 0);
            consumer.assign(List.of(topicPartition));
            long currentEndOffset = consumer
                    .endOffsets(List.of(topicPartition))
                    .get(topicPartition);
            consumer.seek(topicPartition, currentEndOffset);

            OutboxEvent savedEvent = outboxEventRepository.saveAndFlush(outboxEvent);

            try {
                outboxPublisher.publishPendingEvents();

                OutboxEvent publishedEvent = outboxEventRepository
                        .findById(savedEvent.getId())
                        .orElseThrow();
                ConsumerRecord<String, String> publishedRecord =
                        waitForTransfer(consumer, event.transferId());

                assertThat(publishedEvent.getPublishedAt()).isNotNull();
                assertThat(publishedRecord.key())
                        .isEqualTo(event.transferId().toString());
                assertThat(publishedRecord.value())
                        .contains(event.eventId().toString())
                        .contains(event.transferId().toString());
            } finally {
                outboxEventRepository.deleteById(savedEvent.getId());
            }
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "outbox-publisher-test-" + UUID.randomUUID()
        );
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        return new KafkaConsumer<>(properties);
    }

    private ConsumerRecord<String, String> waitForTransfer(
            KafkaConsumer<String, String> consumer,
            UUID transferId
    ) {
        Instant deadline = Instant.now().plusSeconds(10);

        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(500));

            for (ConsumerRecord<String, String> record : records) {
                if (transferId.toString().equals(record.key())) {
                    return record;
                }
            }
        }

        throw new AssertionError("Outbox event was not published within 10 seconds");
    }

    private TransferCompletedEvent transferCompletedEvent() {
        return new TransferCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("25.50"),
                Currency.EUR,
                Instant.now()
        );
    }
}
