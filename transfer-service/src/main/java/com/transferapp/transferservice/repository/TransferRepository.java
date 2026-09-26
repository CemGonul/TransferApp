package com.transferapp.transferservice.repository;

import com.transferapp.transferservice.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer,UUID> {

    @Query("""
        SELECT t
        FROM Transfer t
        WHERE t.senderAccountId IN :accountIds
           OR t.receiverAccountId IN :accountIds
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    Page<Transfer> findAllByAccountIds(
            @Param("accountIds") List<UUID> accountIds,
            Pageable pageable
    );
}
