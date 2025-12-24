package com.marketplace.search.dto.response;

import java.math.BigDecimal;

/**
 * Price range facet bucket.
 */
public record PriceRangeBucket(
        BigDecimal from,
        BigDecimal to,
        long count,
        String label
) {}
