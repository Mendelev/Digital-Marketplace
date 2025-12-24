package com.marketplace.cart.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cart item response DTO.
 */
@Schema(description = "Cart item details")
public record CartItemResponse(
    @Schema(description = "Cart item unique identifier")
    UUID cartItemId,

    @Schema(description = "Product ID from catalog")
    UUID productId,

    @Schema(description = "Product SKU snapshot")
    String sku,

    @Schema(description = "Product title snapshot")
    String titleSnapshot,

    @Schema(description = "Unit price snapshot at time of add")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    BigDecimal unitPriceSnapshot,

    @Schema(description = "Currency code")
    String currency,

    @Schema(description = "Quantity")
    Integer quantity,

    @Schema(description = "Item subtotal (unitPrice × quantity)")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    BigDecimal subtotal
) {}
