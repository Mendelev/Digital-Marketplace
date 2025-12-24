package com.marketplace.cart.domain.model;

/**
 * Cart status enumeration.
 */
public enum CartStatus {
    ACTIVE,       // Current active cart for user
    CHECKED_OUT,  // Cart has been checked out (immutable snapshot)
    ABANDONED     // Future use: cart has been abandoned (not used in MVP)
}
