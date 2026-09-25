package com.teya.ledger.transaction.service;

import com.teya.ledger.balance.service.BalanceService;
import com.teya.ledger.common.exception.InsufficientFundsException;
import com.teya.ledger.transaction.model.Transaction;
import com.teya.ledger.transaction.model.TransactionType;
import com.teya.ledger.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;

    /**
     * Records a money movement. A withdrawal is rejected if it would make the balance negative.
     */
    public Transaction createTransaction(TransactionType type, BigDecimal amount) {
        if (type == TransactionType.WITHDRAWAL) {
            BigDecimal balance = balanceService.getBalance();
            if (amount.compareTo(balance) > 0) {
                throw new InsufficientFundsException(amount, balance);
            }
        }

        Transaction transaction = new Transaction(UUID.randomUUID(), type, amount, Instant.now());
        return transactionRepository.save(transaction);
    }

    /**
     * Returns all transactions, oldest first.
     */
    public List<Transaction> getTransactions() {
        return transactionRepository.findAll();
    }
}
