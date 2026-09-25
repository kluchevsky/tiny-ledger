package com.teya.ledger.transaction.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A single money movement in the ledger. The amount is always positive;
 * the direction is given by the type.
 */
public record Transaction(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        Instant createdAt
) {
}
