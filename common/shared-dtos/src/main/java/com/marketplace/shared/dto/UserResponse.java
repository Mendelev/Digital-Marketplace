package com.marketplace.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User response DTO shared between Auth Service and User Service.
 * Version: 1.0.0
 */
@Schema(description = "User response")
public record UserResponse(
    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId,
    
    @Schema(description = "Email address", example = "user@example.com")
    String email,
    
    @Schema(description = "Full name", example = "John Doe")
    String name,
    
    @Schema(description = "Phone number in E.164 format", example = "+15551234567")
    String phone,
    
    @Schema(description = "User roles", example = "[\"CUSTOMER\"]")
    List<String> roles,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @Schema(example = "2025-12-17T10:30:00.000+00:00", description = "Creation timestamp in UTC with timezone offset")
    OffsetDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @Schema(example = "2025-12-17T10:30:00.000+00:00", description = "Last update timestamp in UTC with timezone offset")
    OffsetDateTime updatedAt
) {
}
