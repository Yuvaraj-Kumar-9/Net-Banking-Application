package com.hfdcbank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** JSON body for POST /api/auth/forgot-password. */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email
) {
}
