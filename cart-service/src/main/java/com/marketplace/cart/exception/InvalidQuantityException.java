package com.marketplace.cart.exception;

/**
 * Exception thrown for invalid quantity operations.
 */
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(String message) {
        super(message);
    }
}
