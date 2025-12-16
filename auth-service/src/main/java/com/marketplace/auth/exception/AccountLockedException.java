package com.marketplace.auth.exception;

/**
 * Exception thrown when attempting to use a locked account.
 */
public class AccountLockedException extends RuntimeException {
    
    public AccountLockedException() {
        super("Account is locked due to multiple failed login attempts. Please contact support.");
    }
    
    public AccountLockedException(String message) {
        super(message);
    }
}
