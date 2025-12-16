package com.marketplace.auth.exception;

/**
 * Exception thrown when attempting to register with an email that already exists.
 */
public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String email) {
        super("Email address is already registered: " + email);
    }
}
