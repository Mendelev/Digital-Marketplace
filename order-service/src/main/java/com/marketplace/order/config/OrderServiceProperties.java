package com.marketplace.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Configuration properties for Order Service business logic.
 */
@ConfigurationProperties(prefix = "order-service")
public record OrderServiceProperties(
        BigDecimal flatShippingRate,
        Integer paymentSuccessRate,
        Integer reservationTtlMinutes
) {
}
