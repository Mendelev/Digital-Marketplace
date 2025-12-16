package com.marketplace.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for User Service client.
 * Binds to 'user-service' prefix in application.yml
 */
@ConfigurationProperties(prefix = "user-service")
@Validated
public record UserServiceProperties(
    @NotBlank(message = "User service base URL must be configured")
    String baseUrl,
    
    @Min(value = 100, message = "Connect timeout must be at least 100ms")
    int connectTimeoutMs,
    
    @Min(value = 100, message = "Read timeout must be at least 100ms")
    int readTimeoutMs
) {
}
