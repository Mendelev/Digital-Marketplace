package com.marketplace.shipping.exception;

public class ShipmentAlreadyExistsException extends RuntimeException {
    public ShipmentAlreadyExistsException(String message) {
        super(message);
    }
}
