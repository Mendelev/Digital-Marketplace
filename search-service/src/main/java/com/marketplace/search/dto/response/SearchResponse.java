package com.marketplace.search.dto.response;

import java.util.List;

/**
 * Response DTO for product search.
 */
public record SearchResponse(
        List<ProductSearchResult> products,
        long totalResults,
        SearchFacets facets,
        PaginationInfo pagination
) {}
