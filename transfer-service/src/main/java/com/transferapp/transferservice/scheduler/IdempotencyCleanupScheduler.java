package com.transferapp.transferservice.scheduler;

import com.transferapp.transferservice.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {

    private final IdempotencyService idempotencyService;

    @Scheduled(
            cron = "${idempotency.cleanup.cron}",
            zone = "UTC"
    )
    public void cleanupExpiredRequests() {
        idempotencyService.deleteExpiredRequests();
    }
}
