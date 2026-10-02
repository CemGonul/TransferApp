package com.transferapp.transferservice.service;

import com.transferapp.transferservice.dto.CreateTransferRequest;
import com.transferapp.transferservice.dto.TransferResponse;
import com.transferapp.transferservice.entity.Account;
import com.transferapp.transferservice.entity.IdempotencyRequest;
import com.transferapp.transferservice.entity.Transfer;
import com.transferapp.transferservice.exception.*;
import com.transferapp.transferservice.outbox.OutboxService;
import com.transferapp.transferservice.repository.AccountRepository;
import com.transferapp.transferservice.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.transferapp.transferservice.event.TransferCompletedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;

    @Transactional
    public TransferResponse createTransfer(
            CreateTransferRequest request,
            UUID authenticatedUserId,
            String idempotencyKey
    ) {

        IdempotencyService.IdempotencyDecision idempotencyDecision =
                idempotencyService.prepare(
                        request,
                        authenticatedUserId,
                        idempotencyKey
                );

        if (idempotencyDecision.replay()) {
            return getReplayResponse(idempotencyDecision.request());
        }

        Transfer savedTransfer =
                executeTransfer(request, authenticatedUserId);

        idempotencyService.complete(
                idempotencyDecision.request(),
                savedTransfer.getId()
        );

        return toResponse(savedTransfer);
    }

    private Transfer executeTransfer(
            CreateTransferRequest request,
            UUID authenticatedUserId
    ) {

        UUID sourceAccountId = request.sourceAccountId();

        UUID receiverAccountId =
                findReceiverAccountId(
                        request.targetAccountNumber()
                );

        validateDifferentAccountIds(
                sourceAccountId,
                receiverAccountId
        );

        Account[] lockedAccounts = lockAccountsInOrder(
                sourceAccountId,
                receiverAccountId
        );

        Account[] resolvedAccounts =
                resolveSourceAndReceiver(
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

        TransferCompletedEvent event =
                new TransferCompletedEvent(
                        UUID.randomUUID(),
                        savedTransfer.getId(),
                        sourceAccount.getUserId(),
                        receiverAccount.getUserId(),
                        savedTransfer.getAmount(),
                        savedTransfer.getCurrency(),
                        savedTransfer.getCompletedAt()
                );

        outboxService.saveTransferCompletedEvent(event);

        return savedTransfer;
    }

    private TransferResponse getReplayResponse(
            IdempotencyRequest idempotencyRequest
    ) {

        Transfer existingTransfer =
                transferRepository
                        .findById(idempotencyRequest.getTransferId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Transfer linked to idempotency request was not found"
                                )
                        );

        return toResponse(existingTransfer);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> getTransfers(
            UUID userId,
            Pageable pageable
    ) {

        Pageable historyPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        List<Account> accounts =
                accountRepository.findAllByUserIdOrderByCreatedAtAsc(userId);

        if (accounts.isEmpty()) {
            return Page.empty(historyPageable);
        }

        List<UUID> accountIds = new ArrayList<>();

        for (int i = 0; i < accounts.size(); i++) {
            accountIds.add(accounts.get(i).getId());
        }

        Page<Transfer> transfers =
                transferRepository.findAllByAccountIds(
                        accountIds,
                        historyPageable
                );

        return transfers.map(this::toResponse);
    }
    @Transactional(readOnly = true)
    public TransferResponse getTransferById(
            UUID transferId,
            UUID userId
    ) {

        Transfer transfer = transferRepository
                .findById(transferId)
                .orElseThrow(() ->
                        new TransferNotFoundException(
                                "Transfer not found"
                        )
                );

        List<Account> accounts =
                accountRepository.findAllByUserIdOrderByCreatedAtAsc(userId);

        List<UUID> accountIds = new ArrayList<>();

        for (int i = 0; i < accounts.size(); i++) {
            accountIds.add(accounts.get(i).getId());
        }

        boolean hasAccess =
                accountIds.contains(transfer.getSenderAccountId())
                        || accountIds.contains(transfer.getReceiverAccountId());

        if (!hasAccess) {
            throw new TransferAccessDeniedException(
                    "You do not have access to this transfer"
            );
        }

        return toResponse(transfer);
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
