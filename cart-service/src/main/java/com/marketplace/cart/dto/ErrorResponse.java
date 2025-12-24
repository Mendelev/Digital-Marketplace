package com.marketplace.cart.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Standard error response DTO.
 */
@Schema(description = "Error response")
public record ErrorResponse(
    @Schema(description = "HTTP status code")
    int status,

    @Schema(description = "Error type")
    String error,

    @Schema(description = "Error message")
    String message,

    @Schema(description = "Request path")
    String path,

    @Schema(description = "Timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now());
    }
}
