package com.marketplace.shipping.dto;

import com.marketplace.shipping.domain.model.AddressSnapshot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateShipmentRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Shipping address is required")
        AddressSnapshot shippingAddress,

        @NotNull(message = "Item count is required")
        @Positive(message = "Item count must be positive")
        Integer itemCount,

        BigDecimal packageWeightKg,

        String packageDimensions
) {
}
