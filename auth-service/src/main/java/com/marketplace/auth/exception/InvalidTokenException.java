package com.marketplace.auth.exception;

/**
 * Exception thrown when a JWT token is invalid or malformed.
 */
public class InvalidTokenException extends RuntimeException {
    
    public InvalidTokenException() {
        super("Invalid or malformed token");
    }
    
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
