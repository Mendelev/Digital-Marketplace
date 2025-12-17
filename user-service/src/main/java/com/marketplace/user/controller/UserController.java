package com.marketplace.user.controller;

import com.marketplace.user.config.AuthServiceProperties;
import com.marketplace.shared.dto.CreateUserRequest;
import com.marketplace.shared.dto.UpdateUserRequest;
import com.marketplace.shared.dto.UserResponse;
import com.marketplace.user.exception.InvalidSharedSecretException;
import com.marketplace.user.exception.UnauthorizedException;
import com.marketplace.user.security.AuthenticatedUser;
import com.marketplace.user.security.CurrentUser;
import com.marketplace.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for user management.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "APIs for managing user profiles")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final String SERVICE_SECRET_HEADER = "X-Service-Secret";

    private final UserService userService;
    private final AuthServiceProperties authServiceProperties;

    public UserController(UserService userService, AuthServiceProperties authServiceProperties) {
        this.userService = userService;
        this.authServiceProperties = authServiceProperties;
    }

    /**
     * Create user endpoint - called by Auth Service during registration.
     * Requires shared secret header for authorization.
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user (Auth Service only)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "403", description = "Invalid shared secret"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<UserResponse> createUser(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @Valid @RequestBody CreateUserRequest request) {

        log.info("Create user request received for: {}", request.email());

        // Validate shared secret
        if (sharedSecret == null || !sharedSecret.equals(authServiceProperties.sharedSecret())) {
            log.error("Invalid or missing shared secret in create user request");
            throw new InvalidSharedSecretException("Invalid service authentication");
        }

        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get current user profile.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get authenticated user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser AuthenticatedUser authenticatedUser) {
        
        if (authenticatedUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        log.debug("Get current user: {}", authenticatedUser.getUserId());

        UserResponse response = userService.getUserById(authenticatedUser.getUserId(), authenticatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get user profile (self or admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID userId,
            @CurrentUser AuthenticatedUser authenticatedUser) {

        log.debug("Get user by ID: {}", userId);

        UserResponse response = userService.getUserById(userId, authenticatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user profile.
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Update user profile (self or admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            @CurrentUser AuthenticatedUser authenticatedUser) {

        log.debug("Update user: {}", userId);

        UserResponse response = userService.updateUser(userId, request, authenticatedUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete user endpoint - internal operation for compensating transactions.
     * Called by Auth Service when credential save fails during registration.
     * Requires shared secret header for authorization.
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user (Internal)", 
               description = "Delete user - internal operation for Auth Service compensating transactions")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Invalid shared secret"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret) {

        log.info("Delete user request received for userId: {}", userId);

        // Validate shared secret
        if (sharedSecret == null || !sharedSecret.equals(authServiceProperties.sharedSecret())) {
            log.error("Invalid or missing shared secret in delete user request");
            throw new InvalidSharedSecretException("Invalid service authentication");
        }

        userService.deleteUser(userId);
        log.info("User deleted successfully: {}", userId);
        return ResponseEntity.noContent().build();
    }
}
