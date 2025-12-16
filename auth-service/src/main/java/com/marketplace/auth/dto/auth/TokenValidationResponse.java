package com.marketplace.auth.dto.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for token validation.
 */
public record TokenValidationResponse(
        boolean valid,
        UUID userId,
        String email,
        List<String> roles,
        Instant expiresAt
) {
}
