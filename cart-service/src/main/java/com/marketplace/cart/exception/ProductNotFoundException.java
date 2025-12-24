package com.marketplace.cart.exception;

/**
 * Exception thrown when a product is not found in catalog.
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
