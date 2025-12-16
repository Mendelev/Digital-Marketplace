package com.marketplace.user.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request to create a new user (from Auth Service).
 */
public record CreateUserRequest(
    @NotNull(message = "User ID is required")
    UUID userId,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @NotNull(message = "Roles are required")
    @Size(min = 1, message = "At least one role is required")
    List<String> roles
) {
}
