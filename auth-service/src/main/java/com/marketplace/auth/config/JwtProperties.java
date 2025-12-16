package com.marketplace.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for JWT token management.
 * Binds to 'jwt' prefix in application.yml
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
    @NotBlank(message = "Private key path must be configured")
    String privateKeyPath,
    
    @NotBlank(message = "Public key path must be configured")
    String publicKeyPath,
    
    @Min(value = 1, message = "Access token expiration must be at least 1 minute")
    int accessTokenExpirationMinutes,
    
    @Min(value = 1, message = "Refresh token expiration must be at least 1 day")
    int refreshTokenExpirationDays
) {
}
