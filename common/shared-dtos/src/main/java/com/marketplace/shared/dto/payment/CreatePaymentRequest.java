package com.marketplace.shared.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request to create a new payment")
public record CreatePaymentRequest(
    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID orderId,

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Schema(description = "Payment amount", example = "99.99")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    String currency
) {}
