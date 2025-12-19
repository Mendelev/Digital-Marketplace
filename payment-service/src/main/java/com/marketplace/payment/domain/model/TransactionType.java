package com.marketplace.payment.domain.model;

public enum TransactionType {
    AUTHORIZE,  // Hold funds
    CAPTURE,    // Transfer funds
    REFUND,     // Return funds
    VOID        // Cancel hold
}
