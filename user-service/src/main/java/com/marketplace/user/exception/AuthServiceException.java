package com.marketplace.user.exception;

/**
 * Exception thrown when Auth Service is unavailable or returns errors.
 */
public class AuthServiceException extends RuntimeException {
    
    public AuthServiceException(String message) {
        super(message);
    }
    
    public AuthServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
