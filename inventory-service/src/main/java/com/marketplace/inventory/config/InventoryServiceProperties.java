package com.marketplace.inventory.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "inventory-service")
@Validated
public record InventoryServiceProperties(
    @Min(value = 1, message = "Reservation TTL must be at least 1 minute")
    Integer reservationTtlMinutes,

    @Min(value = 0, message = "Low stock threshold must be non-negative")
    Integer defaultLowStockThreshold
) {}
