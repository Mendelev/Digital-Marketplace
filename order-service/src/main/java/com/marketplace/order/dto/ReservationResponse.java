package com.marketplace.order.dto;

import java.util.UUID;

/**
 * Response DTO for inventory reservation operations.
 */
public record ReservationResponse(
        UUID reservationId,
        boolean success,
        String message
) {
}
