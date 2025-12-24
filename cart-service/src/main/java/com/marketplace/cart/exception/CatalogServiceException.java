package com.marketplace.cart.exception;

/**
 * Exception thrown when catalog service is unavailable or returns an error.
 */
public class CatalogServiceException extends RuntimeException {
    public CatalogServiceException(String message) {
        super(message);
    }

    public CatalogServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
