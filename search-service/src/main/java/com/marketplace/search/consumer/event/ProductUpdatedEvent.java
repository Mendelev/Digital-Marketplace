package com.marketplace.search.consumer.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event representing product update.
 */
public record ProductUpdatedEvent(
        UUID eventId,
        String eventType,
        UUID productId,
        String name,
        String description,
        BigDecimal basePrice,
        String categoryName,
        UUID sellerId,
        String status,
        List<String> imageUrls,
        List<String> availableSizes,
        List<String> availableColors,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
