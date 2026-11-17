package com.hfdcbank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Balance enquiry payload. lastUpdated is the most recent ledger entry time. */
public record BalanceResponse(
        String accountNumber,
        String accountType,
        BigDecimal balance,
        LocalDateTime lastUpdated
) {
}
