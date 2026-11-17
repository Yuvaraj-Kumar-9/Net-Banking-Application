package com.hfdcbank.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain record for one row of the customers table.
 * Records are immutable: a row is loaded, used, and discarded - never mutated.
 * The password hash is deliberately included for login verification but is
 * NEVER serialised to JSON (controllers only return ProfileResponse).
 */
public record Customer(
        String customerId,
        String fullName,
        String email,
        String mobileNumber,
        LocalDate dateOfBirth,
        String address,
        String passwordHash,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
