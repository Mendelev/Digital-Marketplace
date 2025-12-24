package com.marketplace.search.dto.response;

/**
 * Facet bucket with value and document count.
 */
public record FacetBucket(
        String value,
        long count
) {}
