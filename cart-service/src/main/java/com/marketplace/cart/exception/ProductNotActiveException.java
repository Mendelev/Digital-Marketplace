package com.marketplace.cart.exception;

/**
 * Exception thrown when a product is not in ACTIVE status.
 */
public class ProductNotActiveException extends RuntimeException {
    public ProductNotActiveException(String message) {
        super(message);
    }
}
