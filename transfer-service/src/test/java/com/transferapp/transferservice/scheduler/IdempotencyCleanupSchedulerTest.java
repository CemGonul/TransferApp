package com.transferapp.transferservice.scheduler;

import com.transferapp.transferservice.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdempotencyCleanupSchedulerTest {

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private IdempotencyCleanupScheduler idempotencyCleanupScheduler;

    @Test
    void delegatesCleanupToIdempotencyService() {
        idempotencyCleanupScheduler.cleanupExpiredRequests();

        verify(idempotencyService).deleteExpiredRequests();
    }
}
