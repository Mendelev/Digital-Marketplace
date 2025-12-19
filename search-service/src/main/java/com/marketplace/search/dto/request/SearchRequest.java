package com.marketplace.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for product search.
 */
@Schema(description = "Product search request with query, filters, sort, and pagination")
public record SearchRequest(
        @Schema(description = "Search query text (searches product names and descriptions)", example = "laptop", nullable = true)
        String query,

        @Schema(description = "Search filters (category, price range, status, etc.)", nullable = true)
        SearchFilters filters,

        @Schema(description = "Sort options (field and direction)", nullable = true)
        SortOptions sort,

        @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "Page size (1-100)", example = "20", defaultValue = "20")
        @Min(1) @Max(100)
        Integer size
) {}
