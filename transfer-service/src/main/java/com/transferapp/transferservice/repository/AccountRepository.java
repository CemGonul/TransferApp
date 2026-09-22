package com.transferapp.transferservice.repository;

import com.transferapp.transferservice.entity.Account;
import com.transferapp.transferservice.enums.Currency;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByUserIdAndCurrency(UUID userId, @NotNull Currency currency);

    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :accountId")
    Optional<Account> findByIdForUpdate(@Param("accountId") UUID accountId);

    @Query("SELECT a.id FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<UUID> findIdByAccountNumber(
            @Param("accountNumber") String accountNumber
    );
}
