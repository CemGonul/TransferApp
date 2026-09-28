package com.transferapp.transferservice.service;

import com.transferapp.transferservice.dto.CreateTransferRequest;
import com.transferapp.transferservice.entity.IdempotencyRequest;
import com.transferapp.transferservice.exception.IdempotencyKeyReuseException;
import com.transferapp.transferservice.exception.IdempotencyRequestInProgressException;
import com.transferapp.transferservice.repository.IdempotencyRequestRepository;
import com.transferapp.transferservice.util.RequestHashGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRequestRepository idempotencyRequestRepository;
    private final RequestHashGenerator requestHashGenerator;

    public IdempotencyDecision prepare(
            CreateTransferRequest request,
            UUID authenticatedUserId,
            String idempotencyKey
    ) {

        String requestHash = requestHashGenerator.generate(request);

        Optional<IdempotencyRequest> existingRequest =
                idempotencyRequestRepository
                        .findByUserIdAndIdempotencyKey(
                                authenticatedUserId,
                                idempotencyKey
                        );

        if (existingRequest.isPresent()) {
            IdempotencyRequest idempotencyRequest = existingRequest.get();

            if (!idempotencyRequest.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReuseException(
                        "Idempotency key has already been used for a different request"
                );
            }

            return new IdempotencyDecision(idempotencyRequest, true);
        }

        IdempotencyRequest idempotencyRequest =
                new IdempotencyRequest(
                        authenticatedUserId,
                        idempotencyKey,
                        requestHash
                );

        try {
            idempotencyRequestRepository.saveAndFlush(idempotencyRequest);
        } catch (DataIntegrityViolationException exception) {
            throw new IdempotencyRequestInProgressException(
                    "A request with this idempotency key is already being processed"
            );
        }

        return new IdempotencyDecision(idempotencyRequest, false);
    }

    @Transactional
    public int deleteExpiredRequests() {
        return idempotencyRequestRepository.deleteAllExpiredBefore(
                Instant.now()
        );
    }

    public void complete(
            IdempotencyRequest idempotencyRequest,
            UUID transferId
    ) {
        idempotencyRequest.attachTransfer(transferId);
    }

    public record IdempotencyDecision(
            IdempotencyRequest request,
            boolean replay
    ) {
    }
}
