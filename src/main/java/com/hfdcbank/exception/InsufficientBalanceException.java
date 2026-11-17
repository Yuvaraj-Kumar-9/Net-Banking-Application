package com.hfdcbank.exception;

/** Thrown before any money moves when the account cannot cover a debit. */
public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
