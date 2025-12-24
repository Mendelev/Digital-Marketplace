package com.marketplace.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Standard error response for API exceptions.
 */
public record ErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime timestamp,
        Integer status,
        String error,
        String message,
        String path,
        String correlationId,
        Map<String, String> validationErrors
) {
    public ErrorResponse(OffsetDateTime timestamp, Integer status, String error, String message, String path, String correlationId) {
        this(timestamp, status, error, message, path, correlationId, null);
    }
}
