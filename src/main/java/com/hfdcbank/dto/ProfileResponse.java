package com.hfdcbank.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Customer profile. Contains no password hash, PIN hash or internal ids. */
public record ProfileResponse(
        String customerId,
        String fullName,
        String email,
        String mobileNumber,
        LocalDate dateOfBirth,
        String address,
        LocalDateTime createdAt,
        String accountNumber
) {
}
