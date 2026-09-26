package com.transferapp.transferservice.service;

import com.transferapp.transferservice.dto.TransferResponse;
import com.transferapp.transferservice.entity.Account;
import com.transferapp.transferservice.entity.Transfer;
import com.transferapp.transferservice.enums.Currency;
import com.transferapp.transferservice.enums.TransferStatus;
import com.transferapp.transferservice.repository.AccountRepository;
import com.transferapp.transferservice.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransferService transferService;

    @Test
    void returnsPagedTransferHistoryInRepositoryOrder() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID receiverAccountId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-25T12:00:00Z");
        Instant completedAt = Instant.parse("2026-09-25T12:00:01Z");

        Account account = mock(Account.class);
        when(account.getId()).thenReturn(accountId);

        Transfer transfer = mock(Transfer.class);
        when(transfer.getId()).thenReturn(transferId);
        when(transfer.getSenderAccountId()).thenReturn(accountId);
        when(transfer.getReceiverAccountId()).thenReturn(receiverAccountId);
        when(transfer.getAmount()).thenReturn(new BigDecimal("25.00"));
        when(transfer.getCurrency()).thenReturn(Currency.EUR);
        when(transfer.getStatus()).thenReturn(TransferStatus.COMPLETED);
        when(transfer.getCreatedAt()).thenReturn(createdAt);
        when(transfer.getCompletedAt()).thenReturn(completedAt);

        Pageable request = PageRequest.of(
                1,
                5,
                Sort.by("amount").ascending()
        );
        Pageable expectedPageable = PageRequest.of(1, 5);
        Page<Transfer> transferPage = new PageImpl<>(
                List.of(transfer),
                expectedPageable,
                6
        );

        when(accountRepository.findAllByUserIdOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(account));
        when(transferRepository.findAllByAccountIds(
                List.of(accountId),
                expectedPageable
        )).thenReturn(transferPage);

        Page<TransferResponse> result =
                transferService.getTransfers(userId, request);

        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getContent()).containsExactly(
                new TransferResponse(
                        transferId,
                        accountId,
                        receiverAccountId,
                        new BigDecimal("25.00"),
                        Currency.EUR,
                        TransferStatus.COMPLETED,
                        createdAt,
                        completedAt
                )
        );
    }

    @Test
    void returnsEmptyPageWithoutQueryingTransfersWhenUserHasNoAccounts() {
        UUID userId = UUID.randomUUID();
        Pageable request = PageRequest.of(2, 10);

        when(accountRepository.findAllByUserIdOrderByCreatedAtAsc(userId))
                .thenReturn(List.of());

        Page<TransferResponse> result =
                transferService.getTransfers(userId, request);

        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
        verify(transferRepository, never())
                .findAllByAccountIds(List.of(), request);
    }
}
