package com.transferapp.transferservice.dto;

import com.transferapp.transferservice.enums.Currency;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(

        @NotNull
        Currency currency
) {
}
