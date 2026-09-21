package com.transferapp.transferservice.repository;

import com.transferapp.transferservice.entity.Account;
import com.transferapp.transferservice.enums.Currency;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByUserIdAndCurrency(UUID userId, @NotNull Currency currency);

    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
}
