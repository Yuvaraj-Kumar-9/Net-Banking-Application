package com.hfdcbank.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/** JSON body for POST /api/transfers. */
public record TransferRequest(
        @NotBlank(message = "Beneficiary account number is required")
        String beneficiaryAccountNumber,

        @NotBlank(message = "Beneficiary name is required")
        String beneficiaryName,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @DecimalMax(value = "100000.00", message = "Transfer limit is 100,000.00 per transaction")
        BigDecimal amount,

        String remarks,

        @NotBlank(message = "Transaction PIN is required")
        @Pattern(regexp = "^[0-9]{4,6}$", message = "Transaction PIN must be 4 to 6 digits")
        String transactionPin
) {
}
