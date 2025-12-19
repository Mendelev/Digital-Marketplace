package com.marketplace.search.service;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.RangeAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.RangeBucket;
import com.marketplace.search.document.ProductDocument;
import com.marketplace.search.dto.request.SearchRequest;
import com.marketplace.search.dto.response.*;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps Elasticsearch search results to response DTOs.
 */
@Component
public class SearchResponseMapper {

    /**
     * Map SearchHits to SearchResponse.
     */
    public SearchResponse toSearchResponse(SearchHits<ProductDocument> searchHits, SearchRequest request) {
        List<ProductSearchResult> products = searchHits.getSearchHits().stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());

        SearchFacets facets = extractFacets(searchHits);
        PaginationInfo pagination = buildPaginationInfo(searchHits, request);

        return new SearchResponse(
                products,
                searchHits.getTotalHits(),
                facets,
                pagination
        );
    }

    /**
     * Map SearchHit to ProductSearchResult.
     */
    private ProductSearchResult toSearchResult(SearchHit<ProductDocument> hit) {
        ProductDocument doc = hit.getContent();
        return new ProductSearchResult(
                doc.getProductId(),
                doc.getName(),
                doc.getDescription(),
                doc.getBasePrice(),
                doc.getCategoryName(),
                doc.getSellerId(),
                doc.getStatus(),
                doc.getAvailableSizes(),
                doc.getAvailableColors(),
                doc.getThumbnailUrl(),
                doc.getFeatured(),
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                hit.getScore()
        );
    }

    /**
     * Extract facets from search aggregations.
     */
    private SearchFacets extractFacets(SearchHits<ProductDocument> searchHits) {
        AggregationsContainer<?> aggregationsContainer = searchHits.getAggregations();
        if (aggregationsContainer == null) {
            return new SearchFacets(List.of(), List.of(), List.of(), List.of());
        }

        List<FacetBucket> categories = extractTermsFacet(aggregationsContainer, "categories");
        List<PriceRangeBucket> priceRanges = extractPriceRangeFacet(aggregationsContainer, "priceRanges");
        List<FacetBucket> sizes = extractTermsFacet(aggregationsContainer, "sizes");
        List<FacetBucket> colors = extractTermsFacet(aggregationsContainer, "colors");

        return new SearchFacets(categories, priceRanges, sizes, colors);
    }

    /**
     * Extract terms facet from aggregation.
     */
    private List<FacetBucket> extractTermsFacet(AggregationsContainer<?> aggregationsContainer, String aggName) {
        try {
            var aggregation = aggregationsContainer.aggregations().get(aggName);
            if (aggregation != null && aggregation.isSterms()) {
                StringTermsAggregate sterms = aggregation.sterms();
                return sterms.buckets().array().stream()
                        .map(bucket -> new FacetBucket(bucket.key().stringValue(), bucket.docCount()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // Aggregation not found or wrong type, return empty list
        }
        return new ArrayList<>();
    }

    /**
     * Extract price range facet from aggregation.
     */
    private List<PriceRangeBucket> extractPriceRangeFacet(AggregationsContainer<?> aggregationsContainer, String aggName) {
        try {
            var aggregation = aggregationsContainer.aggregations().get(aggName);
            if (aggregation != null && aggregation.isRange()) {
                RangeAggregate range = aggregation.range();
                return range.buckets().array().stream()
                        .map(bucket -> {
                            BigDecimal from = bucket.from() != null ? BigDecimal.valueOf(bucket.from()) : null;
                            BigDecimal to = bucket.to() != null ? BigDecimal.valueOf(bucket.to()) : null;
                            String label = bucket.key() != null ? bucket.key() : buildPriceLabel(from, to);
                            return new PriceRangeBucket(from, to, bucket.docCount(), label);
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // Aggregation not found or wrong type, return empty list
        }
        return new ArrayList<>();
    }

    /**
     * Build price range label.
     */
    private String buildPriceLabel(BigDecimal from, BigDecimal to) {
        if (from == null && to != null) {
            return "Under $" + to;
        } else if (from != null && to == null) {
            return "$" + from + " and above";
        } else if (from != null && to != null) {
            return "$" + from + " - $" + to;
        }
        return "All";
    }

    /**
     * Build pagination information.
     */
    private PaginationInfo buildPaginationInfo(SearchHits<ProductDocument> searchHits, SearchRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        long totalElements = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PaginationInfo(
                page,
                size,
                totalPages,
                totalElements,
                page < totalPages - 1,
                page > 0
        );
    }
}
