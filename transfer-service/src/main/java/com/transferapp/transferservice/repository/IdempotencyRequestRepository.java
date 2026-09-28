package com.transferapp.transferservice.repository;

import com.transferapp.transferservice.entity.IdempotencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRequestRepository extends JpaRepository<IdempotencyRequest, UUID> {

    Optional<IdempotencyRequest> findByUserIdAndIdempotencyKey(
            UUID userId,
            String idempotencyKey
    );

    @Modifying
    @Query("""
            DELETE FROM IdempotencyRequest request
            WHERE request.expiresAt <= :now
            """)
    int deleteAllExpiredBefore(
            @Param("now") Instant now
    );
}
