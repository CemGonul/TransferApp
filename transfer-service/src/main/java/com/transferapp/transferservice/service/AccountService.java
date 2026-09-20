package com.transferapp.transferservice.service;

import com.transferapp.transferservice.dto.CreateAccountRequest;
import com.transferapp.transferservice.dto.CreateAccountResponse;
import com.transferapp.transferservice.entity.Account;
import com.transferapp.transferservice.exception.AccountAlreadyExistsException;
import com.transferapp.transferservice.repository.AccountRepository;
import com.transferapp.transferservice.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public CreateAccountResponse createAccount(
            CreateAccountRequest request,
            UUID userId) {

        if (accountRepository.existsByUserIdAndCurrency(
                userId,
                request.currency())) {

            throw new AccountAlreadyExistsException(
                    "Account already exists for currency: " + request.currency()
            );
        }

        String accountNumber = generateUniqueAccountNumber();

        Account account = new Account(
                userId,
                accountNumber,
                request.currency()
        );

        Account savedAccount = accountRepository.save(account);

        return new CreateAccountResponse(
                savedAccount.getId(),
                savedAccount.getAccountNumber(),
                savedAccount.getCurrency()
        );
    }

    private String generateUniqueAccountNumber() {

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String accountNumber = accountNumberGenerator.generate();

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique account number"
        );
    }
}