package com.marketplace.auth.domain.model;

/**
 * Enum representing the status of a user credential.
 */
public enum CredentialStatus {
    /**
     * Account is active and can be used for authentication
     */
    ACTIVE,
    
    /**
     * Account is locked due to multiple failed login attempts or administrative action
     */
    LOCKED
}
