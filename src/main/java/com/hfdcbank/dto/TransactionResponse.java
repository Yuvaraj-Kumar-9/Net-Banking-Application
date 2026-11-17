package com.hfdcbank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ledger row safe to show in the UI. */
public record TransactionResponse(
        long transactionId,
        String referenceNumber,
        String transactionType,
        BigDecimal amount,
        String description,
        String transactionStatus,
        LocalDateTime createdAt,
        String direction,
        String relatedParty
) {
}
