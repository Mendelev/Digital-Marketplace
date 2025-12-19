package com.marketplace.order.domain.model;

/**
 * Transaction status enumeration.
 */
public enum TransactionStatus {
    /**
     * Transaction completed successfully
     */
    SUCCESS,

    /**
     * Transaction failed
     */
    FAILED,

    /**
     * Transaction is pending
     */
    PENDING
}
