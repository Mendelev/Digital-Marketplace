package com.marketplace.inventory.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStockRequest(
    @NotNull(message = "Available quantity delta is required")
    Integer availableQtyDelta,

    String reason
) {}
