package com.marketplace.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
