package com.marketplace.shared.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request to capture a payment (full or partial)")
public record CapturePaymentRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Schema(description = "Amount to capture (supports partial capture)", example = "99.99")
    BigDecimal amount,

    @Schema(description = "Idempotency key for duplicate prevention", example = "123e4567-e89b-12d3-a456-426614174000")
    String idempotencyKey
) {}
