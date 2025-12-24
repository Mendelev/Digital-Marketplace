package com.marketplace.shared.dto.inventory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
    UUID reservationId,
    UUID orderId,
    String status,
    OffsetDateTime expiresAt,
    List<ReservationLineResponse> lines,
    OffsetDateTime createdAt
) {}
