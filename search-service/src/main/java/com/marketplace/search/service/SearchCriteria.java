package com.marketplace.search.service;

import com.marketplace.search.dto.request.SearchFilters;
import com.marketplace.search.dto.request.SortOptions;

/**
 * Internal search criteria for Elasticsearch queries.
 */
public record SearchCriteria(
        String query,
        SearchFilters filters,
        SortOptions sort,
        Integer page,
        Integer size
) {}
