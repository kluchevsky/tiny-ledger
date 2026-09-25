package com.teya.ledger.common.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(BigDecimal requested, BigDecimal available) {
        super("Insufficient funds: requested " + requested.toPlainString()
                + ", available " + available.toPlainString());
    }
}
