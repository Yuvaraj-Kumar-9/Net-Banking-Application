package com.hfdcbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Fields a customer is allowed to edit. Email and customer id are immutable. */
public record ProfileUpdateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
        String mobileNumber,

        @NotBlank(message = "Address is required")
        String address
) {
}
