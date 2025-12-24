package com.marketplace.payment.security;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated user context holder.
 */
public class AuthenticatedUser {

    private final UUID userId;
    private final String email;
    private final List<String> roles;

    public AuthenticatedUser(UUID userId, String email, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
