package com.hfdcbank.exception;

/** Thrown when a requested account, transaction or beneficiary does not exist. */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
