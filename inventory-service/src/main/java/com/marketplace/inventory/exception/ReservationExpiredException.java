package com.marketplace.inventory.exception;

import java.util.UUID;

public class ReservationExpiredException extends RuntimeException {

    public ReservationExpiredException(UUID reservationId) {
        super("Reservation has expired: " + reservationId);
    }
}
