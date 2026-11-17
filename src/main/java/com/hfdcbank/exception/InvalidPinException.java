package com.hfdcbank.exception;

/** Thrown when a supplied transaction PIN does not match the BCrypt hash. */
public class InvalidPinException extends BusinessException {

    public InvalidPinException() {
        super("Invalid transaction PIN");
    }
}
