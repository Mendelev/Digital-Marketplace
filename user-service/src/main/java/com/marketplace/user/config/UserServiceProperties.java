package com.marketplace.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * Configuration properties for User Service.
 */
@ConfigurationProperties(prefix = "user-service")
@Validated
public record UserServiceProperties(
    @Min(value = 1, message = "Max addresses per user must be at least 1")
    int maxAddressesPerUser
) {
}
