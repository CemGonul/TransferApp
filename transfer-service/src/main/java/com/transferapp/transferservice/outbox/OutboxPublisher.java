package com.transferapp.transferservice.outbox;

import com.transferapp.transferservice.entity.OutboxEvent;
import com.transferapp.transferservice.event.TransferCompletedEvent;
import com.transferapp.transferservice.event.TransferEventPublisher;
import com.transferapp.transferservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final TransferEventPublisher transferEventPublisher;
    private final OutboxService outboxService;
    private final JsonMapper jsonMapper;

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:5000}")
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (OutboxEvent outboxEvent : events) {
            publish(outboxEvent);
        }
    }

    private void publish(OutboxEvent outboxEvent) {

        TransferCompletedEvent event =
                jsonMapper.readValue(
                        outboxEvent.getPayload(),
                        TransferCompletedEvent.class
                );

        try {
            transferEventPublisher
                    .publishTransferCompleted(event)
                    .join();

            outboxService.markPublished(outboxEvent.getId());

            log.info(
                    "Published outbox event {} for transfer {}",
                    outboxEvent.getEventId(),
                    outboxEvent.getAggregateId()
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Failed to publish outbox event {}",
                    outboxEvent.getEventId(),
                    exception
            );
        }
    }
}
