package com.marketplace.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for payment operations.
 */
public record PaymentResponse(
        UUID paymentId,
        String status,
        BigDecimal amount,
        String message
) {
}
