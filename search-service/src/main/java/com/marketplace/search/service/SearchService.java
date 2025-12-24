package com.marketplace.search.service;

import com.marketplace.search.config.SearchProperties;
import com.marketplace.search.document.ProductDocument;
import com.marketplace.search.dto.request.SearchRequest;
import com.marketplace.search.dto.response.SearchResponse;
import com.marketplace.search.exception.SearchException;
import com.marketplace.search.repository.CustomProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for searching products.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final CustomProductSearchRepository customProductSearchRepository;
    private final SearchResponseMapper searchResponseMapper;
    private final SearchProperties searchProperties;

    public SearchService(CustomProductSearchRepository customProductSearchRepository,
                        SearchResponseMapper searchResponseMapper,
                        SearchProperties searchProperties) {
        this.customProductSearchRepository = customProductSearchRepository;
        this.searchResponseMapper = searchResponseMapper;
        this.searchProperties = searchProperties;
    }

    /**
     * Search products with filters, sorting, and pagination.
     */
    public SearchResponse search(SearchRequest request) {
        try {
            log.debug("Searching products with request: {}", request);

            // Validate pagination
            validatePagination(request);

            // Build search criteria
            SearchCriteria criteria = buildCriteria(request);

            // Execute search
            SearchHits<ProductDocument> searchHits = customProductSearchRepository.search(criteria);

            // Map to response
            SearchResponse response = searchResponseMapper.toSearchResponse(searchHits, request);

            log.info("Search completed: {} results found", response.totalResults());
            return response;

        } catch (SearchException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching products", e);
            throw new SearchException("Failed to search products", e);
        }
    }

    /**
     * Get autocomplete suggestions.
     */
    public List<String> getSuggestions(String query) {
        try {
            log.debug("Getting suggestions for query: {}", query);

            if (query == null || query.isBlank()) {
                return List.of();
            }

            List<String> suggestions = customProductSearchRepository.getSuggestions(
                    query,
                    10 // Max 10 suggestions
            );

            log.debug("Found {} suggestions", suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.error("Error getting suggestions", e);
            throw new SearchException("Failed to get suggestions", e);
        }
    }

    /**
     * Validate pagination parameters.
     */
    private void validatePagination(SearchRequest request) {
        if (request.size() != null && request.size() > searchProperties.pagination().maxPageSize()) {
            throw new SearchException(
                    "Page size cannot exceed " + searchProperties.pagination().maxPageSize()
            );
        }
    }

    /**
     * Build search criteria from request.
     */
    private SearchCriteria buildCriteria(SearchRequest request) {
        return new SearchCriteria(
                request.query(),
                request.filters(),
                request.sort(),
                request.page(),
                request.size()
        );
    }
}
