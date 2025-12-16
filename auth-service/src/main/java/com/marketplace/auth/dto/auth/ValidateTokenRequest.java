package com.marketplace.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token validation.
 */
public record ValidateTokenRequest(
        @NotBlank(message = "Token is required")
        String token
) {
}
