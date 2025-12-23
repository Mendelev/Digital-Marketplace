package com.marketplace.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for User Service integration.
 */
@ConfigurationProperties(prefix = "user-service")
public record UserServiceProperties(
        String baseUrl,
        String sharedSecret,
        Integer connectTimeoutMs,
        Integer readTimeoutMs
) {
}
