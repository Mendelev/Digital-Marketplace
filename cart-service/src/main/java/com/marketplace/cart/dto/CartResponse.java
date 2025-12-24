package com.marketplace.cart.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cart response DTO.
 */
@Schema(description = "Shopping cart details")
public record CartResponse(
    @Schema(description = "Cart unique identifier")
    UUID cartId,

    @Schema(description = "User unique identifier")
    UUID userId,

    @Schema(description = "Cart status")
    String status,

    @Schema(description = "Currency code")
    String currency,

    @Schema(description = "List of cart items")
    List<CartItemResponse> items,

    @Schema(description = "Total item count")
    Integer itemCount,

    @Schema(description = "Cart subtotal (sum of all item subtotals)")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    BigDecimal subtotal,

    @Schema(description = "Cart creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,

    @Schema(description = "Cart last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {}
