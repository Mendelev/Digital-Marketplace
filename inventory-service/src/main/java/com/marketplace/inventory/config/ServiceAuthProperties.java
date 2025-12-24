package com.marketplace.inventory.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "service-auth")
@Validated
public record ServiceAuthProperties(
    @NotBlank(message = "Shared secret must be configured")
    String sharedSecret
) {}
