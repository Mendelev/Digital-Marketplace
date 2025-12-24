package com.marketplace.payment.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    String correlationId
) {
    public ErrorResponse(int status, String error, String message, String path, String correlationId) {
        this(LocalDateTime.now(), status, error, message, path, correlationId);
    }
}
