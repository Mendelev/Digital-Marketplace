package com.marketplace.payment.domain.model;

public enum PaymentStatus {
    INITIATED,       // Payment created, not yet authorized
    AUTHORIZED,      // Funds authorized, not captured
    CAPTURED,        // Funds captured from customer
    FAILED,          // Authorization or capture failed
    REFUNDED,        // Funds refunded to customer
    VOIDED           // Authorization cancelled before capture
}
