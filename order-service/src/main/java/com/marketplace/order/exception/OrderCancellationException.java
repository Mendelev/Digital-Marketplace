package com.marketplace.order.exception;

/**
 * Exception thrown when an order cannot be cancelled.
 */
public class OrderCancellationException extends RuntimeException {

    public OrderCancellationException(String message) {
        super(message);
    }

    public OrderCancellationException(String message, Throwable cause) {
        super(message, cause);
    }
}
