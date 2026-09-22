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

        UUID sourceAccountId = request.sourceAccountId();

        UUID receiverAccountId =
                findReceiverAccountId(request.targetAccountNumber());

        validateDifferentAccountIds(
                sourceAccountId,
                receiverAccountId
        );

        Account[] lockedAccounts = lockAccountsInOrder(
                sourceAccountId,
                receiverAccountId
        );

        Account[] resolvedAccounts = resolveSourceAndReceiver(
                lockedAccounts,
                sourceAccountId
        );

        Account sourceAccount = resolvedAccounts[0];
        Account receiverAccount = resolvedAccounts[1];

        validateOwnership(
                sourceAccount,
                authenticatedUserId
        );

        validateSameCurrency(
                sourceAccount,
                receiverAccount
        );

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

    private UUID findReceiverAccountId(String accountNumber) {
        return accountRepository
                .findIdByAccountNumber(accountNumber)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Receiver account not found"
                        )
                );
    }

    private void validateDifferentAccountIds(
            UUID sourceAccountId,
            UUID receiverAccountId
    ) {
        if (sourceAccountId.equals(receiverAccountId)) {
            throw new SelfTransferException(
                    "You cannot transfer money to the same account"
            );
        }
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
    private Account[] lockAccountsInOrder(
            UUID sourceAccountId,
            UUID receiverAccountId
    ) {

        UUID firstId;
        UUID secondId;

        if (sourceAccountId.compareTo(receiverAccountId) < 0) {
            firstId = sourceAccountId;
            secondId = receiverAccountId;
        } else {
            firstId = receiverAccountId;
            secondId = sourceAccountId;
        }

        Account firstAccount = accountRepository
                .findByIdForUpdate(firstId)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account not found")
                );

        Account secondAccount = accountRepository
                .findByIdForUpdate(secondId)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account not found")
                );

        return new Account[]{firstAccount, secondAccount};
    }

    private Account[] resolveSourceAndReceiver(
            Account[] lockedAccounts,
            UUID sourceAccountId
    ) {

        Account firstAccount = lockedAccounts[0];
        Account secondAccount = lockedAccounts[1];

        if (firstAccount.getId().equals(sourceAccountId)) {
            return new Account[]{firstAccount, secondAccount};
        }

        return new Account[]{secondAccount, firstAccount};
    }
}
