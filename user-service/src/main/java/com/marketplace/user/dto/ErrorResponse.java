package com.marketplace.user.dto;

import java.time.Instant;

/**
 * Standard error response format.
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    Instant timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now());
    }
}
