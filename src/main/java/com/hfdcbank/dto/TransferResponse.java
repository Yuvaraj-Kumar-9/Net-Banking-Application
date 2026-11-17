package com.hfdcbank.dto;

import java.math.BigDecimal;

/** Confirmation shown after an atomic fund transfer. */
public record TransferResponse(
        String referenceNumber,
        BigDecimal amount,
        String beneficiaryAccountNumber,
        String beneficiaryName,
        BigDecimal balance,
        String message
) {
}
