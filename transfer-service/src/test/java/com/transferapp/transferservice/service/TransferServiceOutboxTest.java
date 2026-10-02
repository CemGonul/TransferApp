package com.transferapp.transferservice.service;

import com.transferapp.transferservice.dto.CreateTransferRequest;
import com.transferapp.transferservice.entity.Account;
import com.transferapp.transferservice.entity.IdempotencyRequest;
import com.transferapp.transferservice.entity.Transfer;
import com.transferapp.transferservice.enums.Currency;
import com.transferapp.transferservice.enums.TransferStatus;
import com.transferapp.transferservice.event.TransferCompletedEvent;
import com.transferapp.transferservice.outbox.OutboxService;
import com.transferapp.transferservice.repository.AccountRepository;
import com.transferapp.transferservice.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceOutboxTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TransferService transferService;

    @Test
    void savesCompletedTransferEventToOutboxBeforeCompletingIdempotency() {
        UUID sourceAccountId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID receiverAccountId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID senderUserId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("25.50");
        Instant createdAt = Instant.parse("2026-09-30T11:59:59Z");
        Instant completedAt = Instant.parse("2026-09-30T12:00:00Z");
        String idempotencyKey = UUID.randomUUID().toString();
        CreateTransferRequest request = new CreateTransferRequest(
                sourceAccountId,
                "RECEIVER0001",
                amount
        );
        IdempotencyRequest idempotencyRequest = mock(IdempotencyRequest.class);
        Account sourceAccount = account(sourceAccountId, senderUserId);
        Account receiverAccount = account(receiverAccountId, receiverUserId);
        Transfer savedTransfer = mock(Transfer.class);

        when(idempotencyService.prepare(request, senderUserId, idempotencyKey))
                .thenReturn(new IdempotencyService.IdempotencyDecision(
                        idempotencyRequest,
                        false
                ));
        when(accountRepository.findIdByAccountNumber("RECEIVER0001"))
                .thenReturn(Optional.of(receiverAccountId));
        when(accountRepository.findByIdForUpdate(sourceAccountId))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdForUpdate(receiverAccountId))
                .thenReturn(Optional.of(receiverAccount));
        when(transferRepository.save(any(Transfer.class)))
                .thenReturn(savedTransfer);
        when(savedTransfer.getId()).thenReturn(transferId);
        when(savedTransfer.getSenderAccountId()).thenReturn(sourceAccountId);
        when(savedTransfer.getReceiverAccountId()).thenReturn(receiverAccountId);
        when(savedTransfer.getAmount()).thenReturn(amount);
        when(savedTransfer.getCurrency()).thenReturn(Currency.EUR);
        when(savedTransfer.getStatus()).thenReturn(TransferStatus.COMPLETED);
        when(savedTransfer.getCreatedAt()).thenReturn(createdAt);
        when(savedTransfer.getCompletedAt()).thenReturn(completedAt);

        transferService.createTransfer(request, senderUserId, idempotencyKey);

        ArgumentCaptor<TransferCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(TransferCompletedEvent.class);
        verify(outboxService).saveTransferCompletedEvent(eventCaptor.capture());

        TransferCompletedEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.transferId()).isEqualTo(transferId);
        assertThat(event.senderUserId()).isEqualTo(senderUserId);
        assertThat(event.receiverUserId()).isEqualTo(receiverUserId);
        assertThat(event.amount()).isEqualByComparingTo(amount);
        assertThat(event.currency()).isEqualTo(Currency.EUR);
        assertThat(event.completedAt()).isEqualTo(completedAt);

        InOrder inOrder = inOrder(
                transferRepository,
                outboxService,
                idempotencyService
        );
        inOrder.verify(transferRepository).save(any(Transfer.class));
        inOrder.verify(outboxService).saveTransferCompletedEvent(event);
        inOrder.verify(idempotencyService).complete(
                idempotencyRequest,
                transferId
        );
    }

    private Account account(UUID accountId, UUID userId) {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getUserId()).thenReturn(userId);
        when(account.getCurrency()).thenReturn(Currency.EUR);
        return account;
    }
}
