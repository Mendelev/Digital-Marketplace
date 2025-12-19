package com.marketplace.shared.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for creating an order.
 */
@Schema(description = "Request to create a new order from a shopping cart")
public record CreateOrderRequest(
        @NotNull(message = "User ID is required")
        @Schema(description = "User ID creating the order", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID userId,

        @NotNull(message = "Cart ID is required")
        @Schema(description = "Cart ID to convert to order", example = "660e8400-e29b-41d4-a716-446655440001", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID cartId,

        @NotNull(message = "Shipping address ID is required")
        @Schema(description = "Shipping address ID from user profile", example = "770e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID shippingAddressId,

        @NotNull(message = "Billing address ID is required")
        @Schema(description = "Billing address ID from user profile", example = "880e8400-e29b-41d4-a716-446655440003", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID billingAddressId
) {
}
