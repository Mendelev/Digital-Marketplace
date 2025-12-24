package com.marketplace.shared.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for order item.
 */
@Schema(description = "Order line item with product snapshot and pricing")
public record OrderItemResponse(
        @Schema(description = "Order item ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID orderItemId,

        @Schema(description = "Product ID", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID productId,

        @Schema(description = "Product SKU", example = "LAPTOP-XPS-15")
        String sku,

        @Schema(description = "Product title snapshot at time of order", example = "Dell XPS 15 Laptop - 16GB RAM, 512GB SSD")
        String titleSnapshot,

        @Schema(description = "Unit price snapshot at time of order", example = "99.99")
        BigDecimal unitPriceSnapshot,

        @Schema(description = "Quantity ordered", example = "3")
        Integer quantity,

        @Schema(description = "Line total amount (unitPrice × quantity)", example = "299.97")
        BigDecimal lineTotalAmount
) {
}
