package com.marketplace.shipping.dto;

import java.time.OffsetDateTime;

public record ShipmentTrackingResponse(
        Long id,
        String status,
        String location,
        String description,
        OffsetDateTime eventTime,
        OffsetDateTime createdAt
) {
}
