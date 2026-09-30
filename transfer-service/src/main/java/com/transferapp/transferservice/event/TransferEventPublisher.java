package com.transferapp.transferservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.transferapp.transferservice.config.KafkaTopicConfig.TRANSFER_COMPLETED_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventPublisher {

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    public void publishTransferCompleted(TransferCompletedEvent event) {

        String key = event.transferId().toString();

        kafkaTemplate
                .send(
                        TRANSFER_COMPLETED_TOPIC,
                        key,
                        event)
                .whenComplete((result, exception) -> {

                    if (exception != null) {
                        log.error(
                                "Failed to publish TransferCompletedEvent for transfer {}",
                                event.transferId(),
                                exception
                        );
                        return;
                    }

                    log.info(
                            "Published TransferCompletedEvent for transfer {}",
                            event.transferId()
                    );
                });
    }
}