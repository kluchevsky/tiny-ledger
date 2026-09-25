package com.teya.ledger.balance.controller;

import com.teya.ledger.balance.dto.BalanceResponse;
import com.teya.ledger.balance.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/balance")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping
    public BalanceResponse getBalance() {
        return new BalanceResponse(balanceService.getBalance());
    }
}
