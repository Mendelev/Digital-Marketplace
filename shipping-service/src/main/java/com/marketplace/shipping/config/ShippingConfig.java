package com.marketplace.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "shipping")
public record ShippingConfig(
    BigDecimal flatRateFee,
    String defaultCurrency,
    int estimatedDeliveryDays
) {
}
