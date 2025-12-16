package com.marketplace.user.exception;

/**
 * Exception thrown when shared secret validation fails.
 */
public class InvalidSharedSecretException extends RuntimeException {
    
    public InvalidSharedSecretException(String message) {
        super(message);
    }
}
