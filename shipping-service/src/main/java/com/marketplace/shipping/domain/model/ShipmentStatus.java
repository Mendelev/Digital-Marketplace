package com.marketplace.shipping.domain.model;

public enum ShipmentStatus {
    PENDING,           // Initial state, waiting to be processed
    CREATED,           // Shipment created, label generated
    IN_TRANSIT,        // Package picked up and in transit
    OUT_FOR_DELIVERY,  // Out for final delivery
    DELIVERED,         // Successfully delivered
    CANCELLED,         // Shipment cancelled
    RETURNED           // Returned to sender
}
