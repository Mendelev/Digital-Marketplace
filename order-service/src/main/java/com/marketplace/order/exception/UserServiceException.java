package com.marketplace.order.exception;

/**
 * Exception thrown when communication with User Service fails.
 */
public class UserServiceException extends RuntimeException {

    public UserServiceException(String message) {
        super(message);
    }

    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
