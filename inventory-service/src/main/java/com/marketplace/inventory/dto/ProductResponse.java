package com.marketplace.inventory.dto;

import java.util.UUID;

/**
 * Minimal ProductResponse for Catalog Service integration.
 * Contains only fields needed by Inventory Service.
 */
public record ProductResponse(
    UUID productId,
    String sku,
    String title,
    String status
) {}
