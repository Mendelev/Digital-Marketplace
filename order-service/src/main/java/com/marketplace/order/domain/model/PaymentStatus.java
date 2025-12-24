package com.marketplace.order.domain.model;

/**
 * Payment status enumeration representing the state of a payment transaction.
 */
public enum PaymentStatus {
    /**
     * Payment has been initiated
     */
    INITIATED,

    /**
     * Payment has been authorized (funds reserved)
     */
    AUTHORIZED,

    /**
     * Payment has been captured (funds transferred)
     */
    CAPTURED,

    /**
     * Payment authorization or capture failed
     */
    FAILED,

    /**
     * Payment has been refunded to the customer
     */
    REFUNDED,

    /**
     * Payment authorization has been voided (cancelled before capture)
     */
    VOIDED
}
