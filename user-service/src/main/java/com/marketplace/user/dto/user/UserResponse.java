package com.marketplace.user.dto.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User response DTO.
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
