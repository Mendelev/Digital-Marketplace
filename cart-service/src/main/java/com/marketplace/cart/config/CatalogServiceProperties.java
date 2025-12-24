package com.marketplace.cart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Catalog Service integration.
 */
@ConfigurationProperties(prefix = "catalog-service")
public record CatalogServiceProperties(
    String baseUrl,
    int connectTimeoutMs,
    int readTimeoutMs
) {}
