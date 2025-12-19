package com.marketplace.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStockItemRequest(
    @NotBlank(message = "SKU is required")
    String sku,

    @NotNull(message = "Product ID is required")
    UUID productId,

    @Min(value = 0, message = "Initial quantity must be non-negative")
    Integer initialQty,

    @Min(value = 0, message = "Low stock threshold must be non-negative")
    Integer lowStockThreshold
) {}
