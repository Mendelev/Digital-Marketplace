package com.marketplace.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Cart Service integration.
 */
@ConfigurationProperties(prefix = "cart-service")
public record CartServiceProperties(
        String baseUrl,
        Integer connectTimeoutMs,
        Integer readTimeoutMs
) {
}
