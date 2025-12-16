package com.marketplace.auth.dto.auth;

/**
 * Response DTO for token cleanup operation.
 */
public record CleanupTokensResponse(
        int deletedCount,
        String message
) {
}
