package com.marketplace.search.consumer.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event representing product deletion.
 */
public record ProductDeletedEvent(
        UUID eventId,
        String eventType,
        UUID productId,
        OffsetDateTime deletedAt
) {}
