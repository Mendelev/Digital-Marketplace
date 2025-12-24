package com.marketplace.search.dto.request;

import java.util.List;

/**
 * Filter criteria for product search.
 */
public record SearchFilters(
        List<String> categories,
        PriceRange priceRange,
        List<String> statuses,
        String sellerId,
        List<String> sizes,
        List<String> colors,
        Boolean featured
) {}
