package com.marketplace.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to update user profile.
 * Version: 1.0.0
 */
@Schema(description = "Request to update user profile")
public record UpdateUserRequest(
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Full name", example = "John Doe")
    String name,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be in E.164 format")
    @Schema(description = "Phone number in E.164 format", example = "+15551234567")
    String phone,
    
    @Email(message = "Email must be valid")
    @Schema(description = "Email address", example = "user@example.com")
    String email
) {
}
