package com.marketplace.search.dto.request;

/**
 * Sorting options for product search.
 */
public record SortOptions(
        SortField field,
        SortDirection direction
) {
    public enum SortField {
        RELEVANCE,
        PRICE,
        NEWEST
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}
