package com.marketplace.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Checkout response DTO (immutable cart snapshot).
 */
@Schema(description = "Checkout response with cart snapshot")
public record CheckoutResponse(
    @Schema(description = "Checked-out cart snapshot")
    CartResponse cart,

    @Schema(description = "Success message")
    String message
) {}
