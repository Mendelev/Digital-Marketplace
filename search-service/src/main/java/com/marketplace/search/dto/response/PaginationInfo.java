package com.marketplace.search.dto.response;

/**
 * Pagination information for search results.
 */
public record PaginationInfo(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean hasNext,
        boolean hasPrevious
) {}
