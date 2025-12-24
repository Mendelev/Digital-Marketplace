package com.marketplace.inventory.exception;

public class CatalogServiceException extends RuntimeException {

    public CatalogServiceException(String message) {
        super(message);
    }

    public CatalogServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
