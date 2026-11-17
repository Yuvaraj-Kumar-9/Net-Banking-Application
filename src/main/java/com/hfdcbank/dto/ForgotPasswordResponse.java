package com.hfdcbank.dto;

/**
 * Generic message for all emails (prevents account enumeration). The token is
 * only populated when DEV_MODE=true so the flow can be demonstrated locally.
 */
public record ForgotPasswordResponse(String message, String resetToken) {
}
