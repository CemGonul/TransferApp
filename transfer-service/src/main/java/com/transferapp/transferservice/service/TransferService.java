package com.transferapp.transferservice.service;

import com.transferapp.transferservice.dto.CreateTransferRequest;
import com.transferapp.transferservice.dto.TransferResponse;
import com.transferapp.transferservice.entity.Account;
import com.transferapp.transferservice.entity.Transfer;
import com.transferapp.transferservice.exception.AccountAccessDeniedException;
import com.transferapp.transferservice.exception.AccountNotFoundException;
import com.transferapp.transferservice.exception.CurrencyMismatchException;
import com.transferapp.transferservice.exception.SelfTransferException;
import com.transferapp.transferservice.repository.AccountRepository;
import com.transferapp.transferservice.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public TransferResponse createTransfer(
            CreateTransferRequest request,
            UUID authenticatedUserId
    ) {

        Account sourceAccount =
                findSourceAccount(request.sourceAccountId());

        Account receiverAccount =
                findReceiverAccount(request.targetAccountNumber());

        validateOwnership(sourceAccount, authenticatedUserId);
        validateDifferentAccounts(sourceAccount, receiverAccount);
        validateSameCurrency(sourceAccount, receiverAccount);

        sourceAccount.decreaseBalance(request.amount());
        receiverAccount.increaseBalance(request.amount());

        Transfer transfer = new Transfer(
                sourceAccount.getId(),
                receiverAccount.getId(),
                request.amount(),
                sourceAccount.getCurrency()
        );

        transfer.markCompleted();

        Transfer savedTransfer =
                transferRepository.save(transfer);

        return toResponse(savedTransfer);
    }

    private Account findSourceAccount(UUID sourceAccountId) {

        return accountRepository
                .findById(sourceAccountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Source account not found"
                        )
                );
    }

    private Account findReceiverAccount(String accountNumber) {

        return accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Receiver account not found"
                        )
                );
    }

    private void validateOwnership(
            Account sourceAccount,
            UUID authenticatedUserId
    ) {

        if (!sourceAccount.getUserId().equals(authenticatedUserId)) {
            throw new AccountAccessDeniedException(
                    "You do not have access to the source account"
            );
        }
    }

    private void validateDifferentAccounts(
            Account sourceAccount,
            Account receiverAccount
    ) {

        if (sourceAccount.getId().equals(receiverAccount.getId())) {
            throw new SelfTransferException(
                    "You cannot transfer money to the same account"
            );
        }
    }

    private void validateSameCurrency(
            Account sourceAccount,
            Account receiverAccount
    ) {

        if (sourceAccount.getCurrency() != receiverAccount.getCurrency()) {
            throw new CurrencyMismatchException(
                    "Source and receiver accounts must use the same currency"
            );
        }
    }

    private TransferResponse toResponse(Transfer transfer) {

        return new TransferResponse(
                transfer.getId(),
                transfer.getSenderAccountId(),
                transfer.getReceiverAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }
}