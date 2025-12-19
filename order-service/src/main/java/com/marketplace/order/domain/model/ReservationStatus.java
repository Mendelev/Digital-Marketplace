package com.marketplace.order.domain.model;

/**
 * Reservation status enumeration representing the state of an inventory reservation.
 */
public enum ReservationStatus {
    /**
     * Reservation is active and inventory is reserved
     */
    ACTIVE,

    /**
     * Reservation has been confirmed and stock committed to order
     */
    CONFIRMED,

    /**
     * Reservation has been released and inventory returned to available stock
     */
    RELEASED,

    /**
     * Reservation has expired and inventory should be returned to available stock
     */
    EXPIRED
}
