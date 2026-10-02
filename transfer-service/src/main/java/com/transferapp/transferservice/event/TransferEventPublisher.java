package com.transferapp.transferservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import static com.transferapp.transferservice.config.KafkaTopicConfig.TRANSFER_COMPLETED_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventPublisher {

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    public CompletableFuture<SendResult<String, TransferCompletedEvent>>  publishTransferCompleted(TransferCompletedEvent event) {

        String key = event.transferId().toString();

        return kafkaTemplate.send(
                TRANSFER_COMPLETED_TOPIC,
                key,
                event
        );
    }
}