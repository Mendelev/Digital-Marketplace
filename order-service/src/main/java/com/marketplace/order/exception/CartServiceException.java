package com.marketplace.order.exception;

/**
 * Exception thrown when communication with Cart Service fails.
 */
public class CartServiceException extends RuntimeException {

    public CartServiceException(String message) {
        super(message);
    }

    public CartServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
