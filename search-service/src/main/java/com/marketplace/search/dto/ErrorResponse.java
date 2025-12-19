package com.marketplace.search.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Standard error response for API exceptions.
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {}
