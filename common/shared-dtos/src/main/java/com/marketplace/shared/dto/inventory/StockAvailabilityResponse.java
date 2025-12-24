package com.marketplace.shared.dto.inventory;

import java.util.UUID;

public record StockAvailabilityResponse(
    String sku,
    UUID productId,
    Integer availableQty,
    boolean inStock,
    boolean lowStock
) {}
