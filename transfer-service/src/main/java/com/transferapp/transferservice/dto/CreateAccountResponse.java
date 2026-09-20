package com.transferapp.transferservice.dto;

import com.transferapp.transferservice.enums.Currency;

import java.util.UUID;

public record CreateAccountResponse(
        UUID id,
        String accountNumber,
        Currency currency


) {
}
