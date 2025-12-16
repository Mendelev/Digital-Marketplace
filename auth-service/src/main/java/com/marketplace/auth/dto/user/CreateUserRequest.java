package com.marketplace.auth.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a user in the User Service.
 */
public record CreateUserRequest(
        @NotBlank(message = "User ID is required")
        UUID userId,
        
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        
        @NotBlank(message = "Name is required")
        String name,
        
        @NotEmpty(message = "At least one role is required")
        List<String> roles
) {
}
