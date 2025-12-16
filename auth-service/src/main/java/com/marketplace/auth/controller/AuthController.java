package com.marketplace.auth.controller;

import com.marketplace.auth.dto.ErrorResponse;
import com.marketplace.auth.dto.auth.*;
import com.marketplace.auth.service.AuthService;
import com.marketplace.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Provides endpoints for registration, login, token management, and password recovery.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /**
     * Register a new user.
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account with credentials")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "User Service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.email());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and issue tokens.
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "423", description = "Account locked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get a new access token using a refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiate password reset process.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Request password reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent (if email exists)",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset request received for email: {}", request.email());
        MessageResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset password using reset token.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password with reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired reset token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset confirmation received");
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate JWT token.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate JWT token and return user information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid",
                    content = @Content(schema = @Schema(implementation = TokenValidationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        log.debug("Token validation request received");
        TokenValidationResponse response = authService.validateToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get RSA public key for JWT verification.
     */
    @GetMapping("/public-key")
    @Operation(summary = "Get public key", description = "Get RSA public key in PEM format for JWT verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Public key retrieved successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "500", description = "Error reading public key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<String> getPublicKey() throws java.io.IOException {
        log.debug("Public key request received");
        String publicKey = jwtService.getPublicKeyPem();
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(publicKey);
    }

    /**
     * Cleanup old refresh tokens.
     */
    @DeleteMapping("/tokens/cleanup")
    @Operation(summary = "Cleanup tokens", description = "Delete old refresh tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens cleaned up successfully",
                    content = @Content(schema = @Schema(implementation = CleanupTokensResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CleanupTokensResponse> cleanupTokens(@Valid @RequestBody CleanupTokensRequest request) {
        log.info("Token cleanup request received: olderThanDays={}, includeActive={}", 
                request.olderThanDays(), request.includeActive());
        CleanupTokensResponse response = authService.cleanupTokens(request);
        return ResponseEntity.ok(response);
    }
}
