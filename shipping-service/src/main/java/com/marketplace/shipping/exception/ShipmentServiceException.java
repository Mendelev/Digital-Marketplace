package com.marketplace.shipping.exception;

public class ShipmentServiceException extends RuntimeException {
    public ShipmentServiceException(String message) {
        super(message);
    }

    public ShipmentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
