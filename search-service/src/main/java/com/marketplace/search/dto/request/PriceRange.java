package com.marketplace.search.dto.request;

import java.math.BigDecimal;

/**
 * Price range filter for product search.
 */
public record PriceRange(
        BigDecimal min,
        BigDecimal max
) {}
