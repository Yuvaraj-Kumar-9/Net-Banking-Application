package com.hfdcbank.dto;

import java.math.BigDecimal;

/** Confirmation returned by deposit and withdrawal. */
public record TransactionResult(
        String referenceNumber,
        long transactionId,
        BigDecimal balance,
        String message
) {
}
