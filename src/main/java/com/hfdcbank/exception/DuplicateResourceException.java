package com.hfdcbank.exception;

/** Thrown for duplicate email, mobile number or beneficiary entries. */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
