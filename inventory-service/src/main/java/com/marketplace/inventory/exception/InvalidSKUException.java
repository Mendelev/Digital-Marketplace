package com.marketplace.inventory.exception;

public class InvalidSKUException extends RuntimeException {

    public InvalidSKUException(String sku) {
        super("Invalid SKU: " + sku);
    }
}
