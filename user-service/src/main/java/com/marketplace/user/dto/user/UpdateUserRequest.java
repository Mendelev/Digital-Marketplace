package com.marketplace.user.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to update user profile.
 */
public record UpdateUserRequest(
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be in E.164 format")
    String phone,
    
    @Email(message = "Email must be valid")
    String email
) {
}
