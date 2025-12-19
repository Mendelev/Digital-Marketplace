package com.marketplace.search.dto.response;

import java.util.List;

/**
 * Facet aggregations for product search.
 */
public record SearchFacets(
        List<FacetBucket> categories,
        List<PriceRangeBucket> priceRanges,
        List<FacetBucket> sizes,
        List<FacetBucket> colors
) {}
