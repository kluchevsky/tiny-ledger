package com.teya.ledger.transaction;

import com.teya.ledger.balance.service.BalanceService;
import com.teya.ledger.common.exception.InsufficientFundsException;
import com.teya.ledger.transaction.model.Transaction;
import com.teya.ledger.transaction.model.TransactionType;
import com.teya.ledger.transaction.repository.TransactionRepository;
import com.teya.ledger.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionServiceTest {

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        TransactionRepository transactionRepository = new TransactionRepository();
        transactionService = new TransactionService(transactionRepository, new BalanceService(transactionRepository));
    }

    @Test
    void shouldCreateDepositTransaction() {
        Transaction transaction = transactionService.createTransaction(TransactionType.DEPOSIT, new BigDecimal("100.00"));

        assertThat(transaction.id()).isNotNull();
        assertThat(transaction.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transaction.amount()).isEqualByComparingTo("100.00");
        assertThat(transaction.createdAt()).isNotNull();
    }

    @Test
    void shouldCreateWithdrawalTransaction() {
        transactionService.createTransaction(TransactionType.DEPOSIT, new BigDecimal("100.00"));

        Transaction transaction = transactionService.createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("40.00"));

        assertThat(transaction.type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(transaction.amount()).isEqualByComparingTo("40.00");
    }

    @Test
    void shouldAllowWithdrawalOfEntireBalance() {
        transactionService.createTransaction(TransactionType.DEPOSIT, new BigDecimal("50.00"));

        Transaction transaction = transactionService.createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("50.00"));

        assertThat(transaction.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldRejectWithdrawalExceedingBalance() {
        transactionService.createTransaction(TransactionType.DEPOSIT, new BigDecimal("50.00"));

        assertThatThrownBy(() -> transactionService.createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Insufficient funds: requested 100.00, available 50.00");
    }

    @Test
    void shouldNotRecordRejectedWithdrawal() {
        transactionService.createTransaction(TransactionType.DEPOSIT, new BigDecimal("50.00"));

        assertThatThrownBy(() -> transactionService.createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(transactionService.getTransactions()).hasSize(1);
    }

    @Test
    void shouldReturnTransactionHistoryOldestFirst() {
        Transaction first = transactionService.createTransaction(TransactionType.DEPOSIT, new BigDecimal("100.00"));
        Transaction second = transactionService.createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("30.00"));
        Transaction third = transactionService.createTransaction(TransactionType.DEPOSIT, new BigDecimal("5.50"));

        assertThat(transactionService.getTransactions()).containsExactly(first, second, third);
    }

    @Test
    void shouldReturnEmptyHistoryWhenNoTransactions() {
        assertThat(transactionService.getTransactions()).isEmpty();
    }
}
