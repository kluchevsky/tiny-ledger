package com.teya.ledger.transaction.dto;

import com.teya.ledger.transaction.model.Transaction;
import com.teya.ledger.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        Instant createdAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.id(),
                transaction.type(),
                transaction.amount(),
                transaction.createdAt()
        );
    }
}
