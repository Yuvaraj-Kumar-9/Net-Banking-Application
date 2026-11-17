package com.hfdcbank.dto;

import java.time.LocalDateTime;

/** Beneficiary view returned by the API (no customer id leakage). */
public record BeneficiaryResponse(
        long beneficiaryId,
        String beneficiaryName,
        String beneficiaryAccountNumber,
        LocalDateTime createdAt
) {
}
