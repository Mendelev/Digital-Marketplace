package com.marketplace.order.domain.model;

/**
 * Payment transaction type enumeration.
 */
public enum TransactionType {
    /**
     * Authorization transaction - reserves funds
     */
    AUTHORIZE,

    /**
     * Capture transaction - transfers authorized funds
     */
    CAPTURE,

    /**
     * Refund transaction - returns funds to customer
     */
    REFUND,

    /**
     * Void transaction - cancels authorization before capture
     */
    VOID
}
