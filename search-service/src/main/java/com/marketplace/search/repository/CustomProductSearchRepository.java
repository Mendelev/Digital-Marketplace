package com.marketplace.search.repository;

import com.marketplace.search.service.SearchCriteria;
import org.springframework.data.elasticsearch.core.SearchHits;
import com.marketplace.search.document.ProductDocument;

import java.util.List;

/**
 * Custom repository for complex product search operations.
 */
public interface CustomProductSearchRepository {

    /**
     * Perform complex search with filters, sorting, and aggregations.
     */
    SearchHits<ProductDocument> search(SearchCriteria criteria);

    /**
     * Get autocomplete suggestions based on product names.
     */
    List<String> getSuggestions(String query, int maxResults);
}
