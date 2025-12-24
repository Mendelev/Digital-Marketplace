package com.marketplace.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order-service")
public record OrderServiceProperties(
    String baseUrl,
    String sharedSecret,
    int connectTimeoutMs,
    int readTimeoutMs
) {
}
