package com.teya.ledger.balance.service;

import com.teya.ledger.transaction.model.Transaction;
import com.teya.ledger.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * The balance is not stored separately; it is always derived from the transaction history,
 * so there is a single source of truth.
 */
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final TransactionRepository transactionRepository;

    public BigDecimal getBalance() {
        return transactionRepository.findAll().stream()
                .map(BalanceService::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal signedAmount(Transaction transaction) {
        return switch (transaction.type()) {
            case DEPOSIT -> transaction.amount();
            case WITHDRAWAL -> transaction.amount().negate();
        };
    }
}
