package com.marketplace.order.domain.model;

/**
 * Order status enumeration representing the state of an order in its lifecycle.
 */
public enum OrderStatus {
    /**
     * Order created, awaiting payment authorization
     */
    PENDING_PAYMENT,

    /**
     * Payment has been authorized but not yet captured
     */
    PAYMENT_AUTHORIZED,

    /**
     * Payment authorization failed
     */
    PAYMENT_FAILED,

    /**
     * Inventory has been reserved for this order
     */
    INVENTORY_RESERVED,

    /**
     * Order confirmed - payment authorized and inventory reserved
     */
    CONFIRMED,

    /**
     * Order has been shipped to the customer
     */
    SHIPPED,

    /**
     * Order has been delivered to the customer
     */
    DELIVERED,

    /**
     * Order has been cancelled
     */
    CANCELLED,

    /**
     * Order has been refunded
     */
    REFUNDED
}
