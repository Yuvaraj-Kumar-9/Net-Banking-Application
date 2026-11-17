package com.hfdcbank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Public account details - excludes the PIN hash. */
public record AccountResponse(
        long accountId,
        String customerId,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String accountStatus,
        LocalDateTime createdAt
) {
}
