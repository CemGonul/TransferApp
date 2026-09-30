package com.transferapp.notificationservice.event;

import com.transferapp.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "transfer-completed")
    public void consume(TransferCompletedEvent event) {

        log.info(
                "Received TransferCompletedEvent: eventId={}, transferId={}",
                event.eventId(),
                event.transferId()
        );

        notificationService.handleTransferCompleted(event);
    }
}

