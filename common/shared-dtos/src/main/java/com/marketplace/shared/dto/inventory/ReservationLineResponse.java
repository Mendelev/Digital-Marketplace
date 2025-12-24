package com.marketplace.shared.dto.inventory;

import java.util.UUID;

public record ReservationLineResponse(
    UUID reservationLineId,
    String sku,
    Integer quantity
) {}
