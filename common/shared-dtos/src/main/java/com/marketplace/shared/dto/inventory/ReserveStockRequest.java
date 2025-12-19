package com.marketplace.shared.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReserveStockRequest(
    @NotNull(message = "Order ID is required")
    UUID orderId,

    @NotEmpty(message = "At least one reservation line is required")
    @Valid
    List<ReservationLineRequest> lines
) {}
