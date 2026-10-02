package com.transferapp.transferservice.outbox;

import com.transferapp.transferservice.entity.OutboxEvent;
import com.transferapp.transferservice.event.TransferCompletedEvent;
import com.transferapp.transferservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String TRANSFER_COMPLETED =
            "TRANSFER_COMPLETED";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public void saveTransferCompletedEvent(
            TransferCompletedEvent event
    ) {

        String payload =
                jsonMapper.writeValueAsString(event);

        OutboxEvent outboxEvent =
                new OutboxEvent(
                        event.eventId(),
                        event.transferId(),
                        TRANSFER_COMPLETED,
                        payload
                );

        outboxEventRepository.save(outboxEvent);
    }

    @Transactional
    public void markPublished(UUID outboxEventId) {

        OutboxEvent outboxEvent = outboxEventRepository
                .findById(outboxEventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Outbox event not found: " + outboxEventId
                        )
                );

        outboxEvent.markPublished(Instant.now());
    }
}
