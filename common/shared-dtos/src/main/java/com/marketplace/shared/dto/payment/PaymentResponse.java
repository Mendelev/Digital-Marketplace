package com.marketplace.shared.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Payment response")
public record PaymentResponse(
    @Schema(description = "Payment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID paymentId,

    @Schema(description = "Order ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID orderId,

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId,

    @Schema(description = "Payment status", example = "AUTHORIZED")
    String status,

    @Schema(description = "Total authorized amount", example = "99.99")
    BigDecimal amount,

    @Schema(description = "Amount captured so far", example = "99.99")
    BigDecimal capturedAmount,

    @Schema(description = "Amount refunded so far", example = "0.00")
    BigDecimal refundedAmount,

    @Schema(description = "Currency code", example = "USD")
    String currency,

    @Schema(description = "Payment provider", example = "MOCK")
    String provider,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @Schema(description = "Creation timestamp", example = "2025-12-19T10:30:00.000+00:00")
    OffsetDateTime createdAt,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @Schema(description = "Last update timestamp", example = "2025-12-19T10:30:00.000+00:00")
    OffsetDateTime updatedAt
) {}
