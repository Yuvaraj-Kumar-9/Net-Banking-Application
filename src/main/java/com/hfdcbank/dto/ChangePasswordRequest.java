package com.hfdcbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** JSON body for PUT /api/profile/change-password. */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
                message = "Password must be 8+ chars with upper, lower, digit and special character")
        String newPassword,

        @NotBlank(message = "Confirm new password is required")
        String confirmPassword
) {
}
