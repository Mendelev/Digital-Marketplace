package com.marketplace.payment.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "auth-service")
@Validated
public record AuthServiceProperties(
    @NotBlank(message = "Auth service base URL must be configured")
    String baseUrl,

    @NotBlank(message = "Shared secret must be configured")
    String sharedSecret
) {}
