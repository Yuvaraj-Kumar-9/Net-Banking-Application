package com.hfdcbank.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** JSON body for POST /api/transactions/deposit (simulated money only). */
public record DepositRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @DecimalMax(value = "1000000.00", message = "Deposit limit is 1,000,000.00 per transaction")
        BigDecimal amount,

        String description
) {
}
