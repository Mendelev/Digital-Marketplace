package com.marketplace.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to add item to cart.
 */
@Schema(description = "Request to add product to cart")
public record AddItemRequest(
    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID from catalog", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID productId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity to add", example = "2", minimum = "1")
    Integer quantity
) {}
