package com.marketplace.shipping.domain.model;

/**
 * Address snapshot stored as JSONB in the shipment.
 * Represents a frozen copy of a shipping address at the time of shipment creation.
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
