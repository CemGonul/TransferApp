package com.transferapp.notificationservice.service;

import com.transferapp.notificationservice.entity.Notification;
import com.transferapp.notificationservice.enums.NotificationType;
import com.transferapp.notificationservice.event.TransferCompletedEvent;
import com.transferapp.notificationservice.repository.NotificationRepository;
import com.transferapp.notificationservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void handleTransferCompleted(TransferCompletedEvent event) {

        int inserted =
                processedEventRepository.tryMarkProcessed(
                        event.eventId(),
                        Instant.now()
                );

        if (inserted == 0) {
            return;
        }

        Notification senderNotification =
                new Notification(
                        event.eventId(),
                        event.senderUserId(),
                        event.transferId(),
                        NotificationType.TRANSFER_SENT,
                        "You sent " + event.amount() + " " + event.currency()
                );

        Notification receiverNotification =
                new Notification(
                        event.eventId(),
                        event.receiverUserId(),
                        event.transferId(),
                        NotificationType.TRANSFER_RECEIVED,
                        "You received " + event.amount() + " " + event.currency()
                );

        notificationRepository.saveAll(
                List.of(
                        senderNotification,
                        receiverNotification
                )
        );
    }
}