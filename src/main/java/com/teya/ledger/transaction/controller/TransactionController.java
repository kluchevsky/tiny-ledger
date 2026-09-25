package com.teya.ledger.transaction.controller;

import com.teya.ledger.transaction.dto.CreateTransactionRequest;
import com.teya.ledger.transaction.dto.TransactionResponse;
import com.teya.ledger.transaction.model.Transaction;
import com.teya.ledger.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request.type(), request.amount());
        return TransactionResponse.from(transaction);
    }

    @GetMapping
    public List<TransactionResponse> getTransactions() {
        return transactionService.getTransactions().stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
