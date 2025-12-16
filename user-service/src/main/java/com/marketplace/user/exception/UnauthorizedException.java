package com.marketplace.user.exception;

/**
 * Exception thrown when authentication is required but not provided.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
}
