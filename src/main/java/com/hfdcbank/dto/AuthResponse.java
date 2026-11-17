package com.hfdcbank.dto;

/** Safe authentication payload - never contains hashes or tokens (those are cookies). */
public record AuthResponse(
        String email,
        String fullName,
        String customerId,
        String accountNumber,
        String message
) {
}
