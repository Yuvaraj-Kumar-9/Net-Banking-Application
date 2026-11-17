package com.hfdcbank.exception;

/** Base class for expected, user-facing banking errors (HTTP 400/404/409). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
