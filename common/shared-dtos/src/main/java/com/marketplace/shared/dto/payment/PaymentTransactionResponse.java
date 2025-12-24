package com.marketplace.shared.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Payment transaction response")
public record PaymentTransactionResponse(
    @Schema(description = "Transaction ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID transactionId,

    @Schema(description = "Payment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID paymentId,

    @Schema(description = "Transaction type", example = "AUTHORIZE")
    String type,

    @Schema(description = "Transaction status", example = "SUCCESS")
    String status,

    @Schema(description = "Transaction amount", example = "99.99")
    BigDecimal amount,

    @Schema(description = "Currency code", example = "USD")
    String currency,

    @Schema(description = "Provider reference ID", example = "mock-txn-123456")
    String providerReference,

    @Schema(description = "Error message if failed")
    String errorMessage,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @Schema(description = "Transaction timestamp", example = "2025-12-19T10:30:00.000+00:00")
    OffsetDateTime createdAt
) {}
