package com.marketplace.auth.dto.auth;

import jakarta.validation.constraints.Min;

/**
 * Request DTO for token cleanup.
 */
public record CleanupTokensRequest(
        @Min(value = 1, message = "Days must be at least 1")
        Integer olderThanDays,
        
        Boolean includeActive
) {
    public CleanupTokensRequest {
        if (olderThanDays == null) {
            olderThanDays = 30;
        }
        if (includeActive == null) {
            includeActive = false;
        }
    }
}
