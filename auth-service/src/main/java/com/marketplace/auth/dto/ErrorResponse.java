package com.marketplace.auth.dto;

import java.time.LocalDateTime;

/**
 * Standard error response DTO matching the common schema.
 */
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
