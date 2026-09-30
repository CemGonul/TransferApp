package com.transferapp.transferservice.event;

import com.transferapp.transferservice.enums.Currency;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.transferapp.transferservice.config.KafkaTopicConfig.TRANSFER_COMPLETED_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransferEventPublisherIntegrationTest {

    @Autowired
    private TransferEventPublisher transferEventPublisher;

    @Test
    void publishesTransferCompletedEventWithTransferIdAsKey() {
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();

        TransferCompletedEvent event = new TransferCompletedEvent(
                eventId,
                transferId,
                senderUserId,
                receiverUserId,
                new BigDecimal("25.50"),
                Currency.EUR,
                Instant.now()
        );

        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            TopicPartition topicPartition =
                    new TopicPartition(TRANSFER_COMPLETED_TOPIC, 0);
            consumer.assign(List.of(topicPartition));
            long currentEndOffset = consumer
                    .endOffsets(List.of(topicPartition))
                    .get(topicPartition);
            consumer.seek(topicPartition, currentEndOffset);

            transferEventPublisher.publishTransferCompleted(event);

            ConsumerRecord<String, String> publishedRecord =
                    waitForTransfer(consumer, transferId);

            assertThat(publishedRecord.key()).isEqualTo(transferId.toString());
            assertThat(publishedRecord.value())
                    .contains(eventId.toString())
                    .contains(transferId.toString())
                    .contains(senderUserId.toString())
                    .contains(receiverUserId.toString())
                    .contains("25.50")
                    .contains("EUR")
                    .contains("completedAt");
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ConsumerConfig.GROUP_ID_CONFIG, "transfer-publisher-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
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

        throw new AssertionError("Transfer event was not published within 10 seconds");
    }
}
