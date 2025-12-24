package com.marketplace.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to update cart item quantity.
 */
@Schema(description = "Request to update cart item quantity")
public record UpdateItemRequest(
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be non-negative (0 to remove)")
    @Schema(description = "New quantity (0 to remove item)", example = "3", minimum = "0")
    Integer quantity
) {}
