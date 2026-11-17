package com.hfdcbank.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the accounts table. Money is BigDecimal, never double/float. */
public record Account(
        long accountId,
        String customerId,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String transactionPinHash,
        String accountStatus,
        LocalDateTime createdAt
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(accountStatus);
    }
}
