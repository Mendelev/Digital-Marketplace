package com.marketplace.user.exception;

/**
 * Exception thrown when email already exists in system.
 */
public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String message) {
        super(message);
    }
}
