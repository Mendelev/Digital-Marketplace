package com.marketplace.auth.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO from User Service after user creation.
 */
public record UserResponse(
        UUID userId,
        String email,
        String name,
        String phone,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
