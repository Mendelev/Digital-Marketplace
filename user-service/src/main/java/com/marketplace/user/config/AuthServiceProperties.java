package com.marketplace.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for Auth Service integration.
 */
@ConfigurationProperties(prefix = "auth-service")
@Validated
public record AuthServiceProperties(
    @NotBlank(message = "Auth service base URL must be configured")
    String baseUrl,
    
    @NotBlank(message = "Public key endpoint must be configured")
    String publicKeyEndpoint,
    
    @Min(value = 1, message = "Cache TTL must be at least 1 minute")
    int publicKeyCacheTtlMinutes,
    
    @NotBlank(message = "Shared secret must be configured")
    String sharedSecret,
    
    @Min(value = 100, message = "Connect timeout must be at least 100ms")
    int connectTimeoutMs,
    
    @Min(value = 100, message = "Read timeout must be at least 100ms")
    int readTimeoutMs
) {
}
