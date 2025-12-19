package com.marketplace.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for obtaining JWT tokens")
public class AuthController {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthController(
            RestTemplate restTemplate,
            @Value("${auth-service.base-url:http://localhost:8080}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    @Operation(
            summary = "Login to get JWT token",
            description = "Authenticate with username and password to receive a JWT token. Use the returned accessToken in the Authorize button above."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful - Copy the accessToken and click 'Authorize' button to use it",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Transform request to Auth Service format (email instead of username)
            Map<String, String> authRequest = Map.of(
                    "email", request.username(),
                    "password", request.password()
            );
            
            HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(authRequest, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    authServiceUrl + "/api/v1/auth/login",
                    HttpMethod.POST,
                    httpEntity,
                    Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Register new user",
            description = "Create a new user account. Note: Roles must be assigned manually in the database after registration."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or user already exists"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<RegisterRequest> httpEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    authServiceUrl + "/api/v1/auth/register",
                    HttpMethod.POST,
                    httpEntity,
                    Map.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Registration failed", "message", e.getMessage()));
        }
    }

    // DTOs
    public record LoginRequest(
            @Schema(description = "Username or email", example = "admin@marketplace.com")
            @NotBlank(message = "Username is required")
            String username,
            
            @Schema(description = "Password", example = "Admin@123")
            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RegisterRequest(
            @Schema(description = "Username", example = "john_doe")
            @NotBlank(message = "Username is required")
            String username,
            
            @Schema(description = "Email address", example = "john@example.com")
            @Email(message = "Invalid email format")
            @NotBlank(message = "Email is required")
            String email,
            
            @Schema(description = "Password (min 8 characters)", example = "SecurePass@123")
            @NotBlank(message = "Password is required")
            String password,
            
            @Schema(description = "First name", example = "John")
            @NotBlank(message = "First name is required")
            String firstName,
            
            @Schema(description = "Last name", example = "Doe")
            @NotBlank(message = "Last name is required")
            String lastName
    ) {}

    @Schema(description = "Login response with JWT tokens")
    public record LoginResponse(
            @Schema(description = "JWT access token - Copy this and use in Authorize button", example = "eyJhbGciOiJIUzI1NiIs...")
            String accessToken,
            
            @Schema(description = "Refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIs...")
            String refreshToken,
            
            @Schema(description = "Token type", example = "Bearer")
            String tokenType,
            
            @Schema(description = "Token expiration time in seconds", example = "3600")
            Long expiresIn
    ) {}
}
