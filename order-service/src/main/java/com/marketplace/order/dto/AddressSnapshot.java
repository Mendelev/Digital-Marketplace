package com.marketplace.order.dto;

/**
 * Address snapshot stored as JSONB in the order.
 * Represents a frozen copy of an address at the time of order creation.
 */
public record AddressSnapshot(
        String label,
        String country,
        String state,
        String city,
        String zip,
        String street,
        String number,
        String complement
) {
}
