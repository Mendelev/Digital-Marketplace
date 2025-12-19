package com.marketplace.shared.dto.inventory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockItemResponse(
    String sku,
    UUID productId,
    Integer availableQty,
    Integer reservedQty,
    Integer totalQty,
    Integer lowStockThreshold,
    boolean isLowStock,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
