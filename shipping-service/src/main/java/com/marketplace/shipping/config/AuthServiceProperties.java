package com.marketplace.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth-service")
public record AuthServiceProperties(
    String baseUrl,
    String sharedSecret
) {
}
