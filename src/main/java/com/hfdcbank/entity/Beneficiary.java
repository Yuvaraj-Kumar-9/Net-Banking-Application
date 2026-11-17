package com.hfdcbank.entity;

import java.time.LocalDateTime;

/** One saved beneficiary belonging to a customer. */
public record Beneficiary(
        long beneficiaryId,
        String customerId,
        String beneficiaryName,
        String beneficiaryAccountNumber,
        LocalDateTime createdAt
) {
}
