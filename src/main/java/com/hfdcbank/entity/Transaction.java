package com.hfdcbank.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One immutable row of the transactions ledger. */
public record Transaction(
        long transactionId,
        String referenceNumber,
        long accountId,
        String transactionType,
        BigDecimal amount,
        String description,
        String transactionStatus,
        LocalDateTime createdAt
) {
    /** CREDIT increases the balance; DEBIT decreases it. The UI uses this badge. */
    public String direction() {
        return switch (transactionType.toUpperCase()) {
            case "DEPOSIT", "TRANSFER_IN", "REFUND" -> "CREDIT";
            default -> "DEBIT";
        };
    }
}
