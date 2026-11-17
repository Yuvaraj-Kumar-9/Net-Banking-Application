package com.hfdcbank.dto;

import java.time.Instant;

/** Uniform JSON error body returned by GlobalExceptionHandler. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
