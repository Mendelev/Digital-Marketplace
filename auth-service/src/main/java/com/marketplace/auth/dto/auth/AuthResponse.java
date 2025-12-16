package com.marketplace.auth.dto.auth;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO containing authentication tokens and user information.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UUID userId,
        String email,
        List<String> roles
) {
}
