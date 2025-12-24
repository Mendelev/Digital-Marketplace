package com.marketplace.inventory.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "catalog-service")
@Validated
public record CatalogServiceProperties(
    @NotBlank(message = "Catalog service base URL must be configured")
    String baseUrl,

    @NotBlank(message = "Shared secret must be configured")
    String sharedSecret,

    @Min(value = 100, message = "Connect timeout must be at least 100ms")
    Integer connectTimeoutMs,

    @Min(value = 100, message = "Read timeout must be at least 100ms")
    Integer readTimeoutMs
) {}
