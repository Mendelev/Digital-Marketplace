package com.marketplace.shipping.dto;

import com.marketplace.shipping.domain.model.AddressSnapshot;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShipmentResponse(
        UUID shipmentId,
        UUID orderId,
        UUID userId,
        String status,
        String trackingNumber,
        String carrier,
        BigDecimal shippingFee,
        String currency,
        Integer itemCount,
        BigDecimal packageWeightKg,
        String packageDimensions,
        AddressSnapshot shippingAddress,
        OffsetDateTime estimatedDeliveryDate,
        OffsetDateTime actualDeliveryDate,
        OffsetDateTime shippedAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
