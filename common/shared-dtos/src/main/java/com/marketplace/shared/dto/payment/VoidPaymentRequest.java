package com.marketplace.shared.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to void a payment authorization")
public record VoidPaymentRequest(
    @Schema(description = "Idempotency key for duplicate prevention", example = "123e4567-e89b-12d3-a456-426614174000")
    String idempotencyKey
) {}
