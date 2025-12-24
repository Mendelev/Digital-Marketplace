package com.marketplace.inventory.exception;

import java.util.UUID;

public class DuplicateReservationException extends RuntimeException {

    public DuplicateReservationException(UUID orderId) {
        super("Reservation already exists for order: " + orderId);
    }
}
