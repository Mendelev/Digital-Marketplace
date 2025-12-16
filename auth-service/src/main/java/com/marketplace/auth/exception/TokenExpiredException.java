package com.marketplace.auth.exception;

/**
 * Exception thrown when a JWT token is expired.
 */
public class TokenExpiredException extends RuntimeException {
    
    public TokenExpiredException() {
        super("Token has expired");
    }
    
    public TokenExpiredException(String message) {
        super(message);
    }
}
