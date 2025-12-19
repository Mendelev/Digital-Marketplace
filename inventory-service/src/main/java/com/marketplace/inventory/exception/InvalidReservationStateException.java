package com.marketplace.inventory.exception;

import com.marketplace.inventory.domain.model.ReservationStatus;

import java.util.UUID;

public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(UUID reservationId, ReservationStatus currentStatus) {
        super(String.format("Invalid state for reservation %s: %s", reservationId, currentStatus));
    }
}
