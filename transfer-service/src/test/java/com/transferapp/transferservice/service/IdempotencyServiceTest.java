package com.transferapp.transferservice.service;

import com.transferapp.transferservice.dto.CreateTransferRequest;
import com.transferapp.transferservice.entity.IdempotencyRequest;
import com.transferapp.transferservice.repository.IdempotencyRequestRepository;
import com.transferapp.transferservice.util.RequestHashGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRequestRepository idempotencyRequestRepository;

    @Mock
    private RequestHashGenerator requestHashGenerator;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    void createsRequestWhenKeyDoesNotExist() {
        UUID userId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        String requestHash = "request-hash";
        CreateTransferRequest request = createRequest();

        when(requestHashGenerator.generate(request)).thenReturn(requestHash);
        when(idempotencyRequestRepository.findByUserIdAndIdempotencyKey(
                userId,
                idempotencyKey
        )).thenReturn(Optional.empty());

        IdempotencyService.IdempotencyDecision decision =
                idempotencyService.prepare(request, userId, idempotencyKey);

        assertThat(decision.replay()).isFalse();
        assertThat(decision.request().getUserId()).isEqualTo(userId);
        assertThat(decision.request().getIdempotencyKey())
                .isEqualTo(idempotencyKey);
        assertThat(decision.request().getRequestHash()).isEqualTo(requestHash);
        verify(requestHashGenerator).generate(request);
        verify(idempotencyRequestRepository)
                .findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        verify(idempotencyRequestRepository)
                .saveAndFlush(decision.request());
        verify(idempotencyRequestRepository, never())
                .deleteAllExpiredBefore(any(Instant.class));
    }

    @Test
    void replaysMatchingExistingRequest() {
        UUID userId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        String requestHash = "request-hash";
        CreateTransferRequest request = createRequest();
        IdempotencyRequest existingRequest =
                new IdempotencyRequest(userId, idempotencyKey, requestHash);

        when(requestHashGenerator.generate(request)).thenReturn(requestHash);
        when(idempotencyRequestRepository.findByUserIdAndIdempotencyKey(
                userId,
                idempotencyKey
        )).thenReturn(Optional.of(existingRequest));

        IdempotencyService.IdempotencyDecision decision =
                idempotencyService.prepare(request, userId, idempotencyKey);

        assertThat(decision.replay()).isTrue();
        assertThat(decision.request()).isSameAs(existingRequest);
        verify(idempotencyRequestRepository, never())
                .saveAndFlush(any(IdempotencyRequest.class));
        verify(idempotencyRequestRepository, never())
                .deleteAllExpiredBefore(any(Instant.class));
    }

    @Test
    void deletesExpiredRequestsInSingleBulkOperation() {
        when(idempotencyRequestRepository.deleteAllExpiredBefore(
                any(Instant.class)
        )).thenReturn(5);

        int deletedRequests = idempotencyService.deleteExpiredRequests();

        assertThat(deletedRequests).isEqualTo(5);
        verify(idempotencyRequestRepository)
                .deleteAllExpiredBefore(any(Instant.class));
    }

    private CreateTransferRequest createRequest() {
        return new CreateTransferRequest(
                UUID.randomUUID(),
                "TARGET-ACCOUNT",
                new BigDecimal("10.00")
        );
    }
}
