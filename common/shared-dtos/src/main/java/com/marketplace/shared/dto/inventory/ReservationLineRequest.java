package com.marketplace.shared.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReservationLineRequest(
    @NotBlank(message = "SKU is required")
    String sku,

    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity
) {}
