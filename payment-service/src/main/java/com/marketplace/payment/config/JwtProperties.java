package com.marketplace.payment.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
    @NotBlank(message = "JWT public key endpoint must be configured")
    String publicKeyEndpoint,

    @Min(value = 1, message = "Public key cache TTL must be at least 1 minute")
    int publicKeyCacheTtlMinutes
) {}
