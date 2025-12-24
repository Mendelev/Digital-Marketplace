package com.marketplace.shared.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for order list views.
 */
@Schema(description = "Order summary for list views with essential information")
public record OrderSummaryResponse(
        @Schema(description = "Order ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID orderId,

        @Schema(description = "User ID who created the order", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID userId,

        @Schema(description = "Order status", example = "CONFIRMED", allowableValues = {
                "PENDING_PAYMENT", "PAYMENT_AUTHORIZED", "INVENTORY_RESERVED", "CONFIRMED",
                "SHIPPED", "DELIVERED", "CANCELLED", "PAYMENT_FAILED", "REFUNDED"
        })
        String status,

        @Schema(description = "Total order amount", example = "309.96")
        BigDecimal totalAmount,

        @Schema(description = "Number of items in order", example = "3")
        Integer itemCount,

        @Schema(description = "Order creation timestamp", example = "2024-01-15T14:30:00.000-03:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createdAt
) {
}
