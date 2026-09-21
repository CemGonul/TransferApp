package com.transferapp.transferservice.service;

import com.transferapp.transferservice.dto.AccountResponse;
import com.transferapp.transferservice.dto.CreateAccountRequest;
import com.transferapp.transferservice.dto.CreateAccountResponse;
import com.transferapp.transferservice.dto.DepositAccountRequest;
import com.transferapp.transferservice.entity.Account;
import com.transferapp.transferservice.exception.AccountAccessDeniedException;
import com.transferapp.transferservice.exception.AccountAlreadyExistsException;
import com.transferapp.transferservice.exception.AccountNotFoundException;
import com.transferapp.transferservice.repository.AccountRepository;
import com.transferapp.transferservice.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

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

        String accountNumber = accountNumberGenerator.generate();

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
    @Transactional(readOnly=true)
    public List<AccountResponse> getAccounts(UUID userId){

        List<Account> accounts=accountRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
        List<AccountResponse> responses=new ArrayList<>();

        for(int i =0;i<accounts.size();i++){
            Account account=accounts.get(i);
            AccountResponse response=new AccountResponse(
                    account.getId(),
                    account.getAccountNumber(),
                    account.getCurrency(),
                    account.getBalance(),
                    account.getCreatedAt());
            responses.add(response);
        }
        return responses;
    }

    @Transactional
    public AccountResponse depositAccount(
            UUID accountId,
            DepositAccountRequest request,
            UUID userId){
        Account account=accountRepository.findById(accountId)
                .orElseThrow(()->new AccountNotFoundException("Account not found"));

        if(!account.getUserId().equals(userId)){
            throw new AccountAccessDeniedException("You do not have acces to this account");
        }
        account.deposit(request.amount());

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
