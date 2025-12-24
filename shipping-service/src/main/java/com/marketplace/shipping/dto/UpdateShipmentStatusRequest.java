package com.marketplace.shipping.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateShipmentStatusRequest(
        @NotNull(message = "Status is required")
        String status,

        String reason
) {
}
