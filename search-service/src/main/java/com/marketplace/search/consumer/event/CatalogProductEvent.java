package com.marketplace.search.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event envelope published by the Catalog Service to Kafka.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProductEvent(
        Long id,
        UUID eventId,
        String eventType,
        UUID productId,
        Long sequenceNumber,
        Map<String, Object> payload,
        OffsetDateTime publishedAt
) {}
