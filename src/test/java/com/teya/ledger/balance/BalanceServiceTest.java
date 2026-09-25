package com.teya.ledger.balance;

import com.teya.ledger.balance.service.BalanceService;
import com.teya.ledger.transaction.model.Transaction;
import com.teya.ledger.transaction.model.TransactionType;
import com.teya.ledger.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceServiceTest {

    private TransactionRepository transactionRepository;
    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        transactionRepository = new TransactionRepository();
        balanceService = new BalanceService(transactionRepository);
    }

    @Test
    void shouldReturnZeroBalanceWhenNoTransactions() {
        assertThat(balanceService.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void shouldReturnCurrentBalance() {
        save(TransactionType.DEPOSIT, "100.00");
        save(TransactionType.WITHDRAWAL, "30.25");
        save(TransactionType.DEPOSIT, "0.10");

        assertThat(balanceService.getBalance()).isEqualByComparingTo("69.85");
    }

    @Test
    void shouldReturnZeroBalanceAfterWithdrawingEverything() {
        save(TransactionType.DEPOSIT, "50.00");
        save(TransactionType.WITHDRAWAL, "50.00");

        assertThat(balanceService.getBalance()).isEqualByComparingTo("0");
    }

    private void save(TransactionType type, String amount) {
        transactionRepository.save(new Transaction(UUID.randomUUID(), type, new BigDecimal(amount), Instant.now()));
    }
}
