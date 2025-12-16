package com.marketplace.auth.exception;

/**
 * Exception thrown when user service operations fail.
 */
public class UserServiceException extends RuntimeException {
    
    public UserServiceException(String message) {
        super(message);
    }
    
    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
