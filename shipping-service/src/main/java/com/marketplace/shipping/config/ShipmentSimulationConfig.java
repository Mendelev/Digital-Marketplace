package com.marketplace.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shipment-simulation")
public record ShipmentSimulationConfig(
    boolean autoProgressEnabled,
    int createdToInTransitDelaySeconds,
    int inTransitToOutForDeliveryDelaySeconds,
    int outForDeliveryToDeliveredDelaySeconds,
    double deliverySuccessRate
) {
}
