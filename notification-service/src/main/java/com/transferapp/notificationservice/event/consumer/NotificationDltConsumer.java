package com.transferapp.notificationservice.event.consumer;

import com.transferapp.notificationservice.config.KafkaConsumerConfig;
import com.transferapp.notificationservice.event.TransferCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationDltConsumer {

    @KafkaListener(
            topics = KafkaConsumerConfig.TRANSFER_COMPLETED_DLT,
            groupId = "notification-service-dlt"
    )
    public void consumeDlt(TransferCompletedEvent event) {

        log.error(
                "TransferCompletedEvent moved to DLT: eventId={}, transferId={}",
                event.eventId(),
                event.transferId()
        );
    }
}
