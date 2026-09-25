package com.teya.ledger.transaction.dto;

import com.teya.ledger.transaction.model.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateTransactionRequest(

        @NotNull
        TransactionType type,

        @NotNull
        @Positive
        BigDecimal amount

) {
}
