package com.teya.ledger.transaction.repository;

import com.teya.ledger.transaction.model.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory transaction storage. Data lives only as long as the application process.
 * Transactions are kept in insertion order, which is also chronological order.
 */
@Repository
public class TransactionRepository {

    private final List<Transaction> transactions = new ArrayList<>();

    public Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    public List<Transaction> findAll() {
        return List.copyOf(transactions);
    }
}
