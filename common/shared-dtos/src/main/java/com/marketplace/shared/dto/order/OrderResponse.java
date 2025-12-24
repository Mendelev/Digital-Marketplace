package com.marketplace.shared.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for order details.
 */
@Schema(description = "Complete order details with items, amounts, addresses, and status")
public record OrderResponse(
        @Schema(description = "Order ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID orderId,

        @Schema(description = "User ID who created the order", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID userId,

        @Schema(description = "Order status", example = "CONFIRMED", allowableValues = {
                "PENDING_PAYMENT", "PAYMENT_AUTHORIZED", "INVENTORY_RESERVED", "CONFIRMED",
                "SHIPPED", "DELIVERED", "CANCELLED", "PAYMENT_FAILED", "REFUNDED"
        })
        String status,

        @Schema(description = "Currency code", example = "USD")
        String currency,

        @Schema(description = "Subtotal amount (sum of all items before shipping/tax/discount)", example = "299.97")
        BigDecimal subtotalAmount,

        @Schema(description = "Shipping amount", example = "9.99")
        BigDecimal shippingAmount,

        @Schema(description = "Tax amount", example = "0.00")
        BigDecimal taxAmount,

        @Schema(description = "Discount amount", example = "0.00")
        BigDecimal discountAmount,

        @Schema(description = "Total amount (subtotal + shipping + tax - discount)", example = "309.96")
        BigDecimal totalAmount,

        @Schema(description = "Payment ID", example = "770e8400-e29b-41d4-a716-446655440002")
        UUID paymentId,

        @Schema(description = "Cart ID used to create this order", example = "880e8400-e29b-41d4-a716-446655440003")
        UUID cartId,

        @Schema(description = "Shipping address snapshot")
        AddressSnapshotResponse shippingAddress,

        @Schema(description = "Billing address snapshot")
        AddressSnapshotResponse billingAddress,

        @Schema(description = "List of order items with product snapshots")
        List<OrderItemResponse> items,

        @Schema(description = "Order creation timestamp", example = "2024-01-15T14:30:00.000-03:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime createdAt,

        @Schema(description = "Order last update timestamp", example = "2024-01-15T15:45:00.000-03:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime updatedAt
) {
}
