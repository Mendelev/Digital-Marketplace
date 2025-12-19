package com.marketplace.search.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.marketplace.search.config.SearchProperties;
import com.marketplace.search.document.ProductDocument;
import com.marketplace.search.dto.request.PriceRange;
import com.marketplace.search.dto.request.SearchFilters;
import com.marketplace.search.dto.request.SortOptions;
import com.marketplace.search.exception.SearchException;
import com.marketplace.search.service.SearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of custom product search repository.
 */
@Repository
public class CustomProductSearchRepositoryImpl implements CustomProductSearchRepository {

    private static final Logger log = LoggerFactory.getLogger(CustomProductSearchRepositoryImpl.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchClient elasticsearchClient;
    private final SearchProperties searchProperties;

    public CustomProductSearchRepositoryImpl(ElasticsearchOperations elasticsearchOperations,
                                            ElasticsearchClient elasticsearchClient,
                                            SearchProperties searchProperties) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.elasticsearchClient = elasticsearchClient;
        this.searchProperties = searchProperties;
    }

    @Override
    public SearchHits<ProductDocument> search(SearchCriteria criteria) {
        try {
            log.debug("Executing search with criteria: {}", criteria);

            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // Add keyword search query
            if (criteria.query() != null && !criteria.query().isBlank()) {
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                        .query(criteria.query())
                        .fields("name^2", "description")
                        .type(TextQueryType.BestFields)
                        .fuzziness("AUTO")
                );
                boolQuery.must(q -> q.multiMatch(multiMatchQuery));
            }

            // Add filters
            if (criteria.filters() != null) {
                addFilters(boolQuery, criteria.filters());
            }

            // Build native query
            org.springframework.data.elasticsearch.core.query.NativeQuery.NativeQueryBuilder queryBuilder =
                    org.springframework.data.elasticsearch.core.query.NativeQuery.builder()
                            .withQuery(q -> q.bool(boolQuery.build()));

            // Add sorting
            if (criteria.sort() != null) {
                addSorting(queryBuilder, criteria.sort());
            } else {
                // Default sort by score if there's a search query
                if (criteria.query() != null && !criteria.query().isBlank()) {
                    queryBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
                }
            }

            // Add pagination
            int page = criteria.page() != null ? criteria.page() : 0;
            int size = criteria.size() != null ? criteria.size() : searchProperties.pagination().defaultPageSize();
            queryBuilder.withPageable(org.springframework.data.domain.PageRequest.of(page, size));

            // Add aggregations for facets
            addAggregations(queryBuilder);

            Query nativeQuery = queryBuilder.build();
            SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

            log.debug("Search returned {} hits", searchHits.getTotalHits());
            return searchHits;

        } catch (Exception e) {
            log.error("Error executing search", e);
            throw new SearchException("Failed to execute search", e);
        }
    }

    @Override
    public List<String> getSuggestions(String query, int maxResults) {
        try {
            log.debug("Getting suggestions for query: {}", query);

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(searchProperties.index().name())
                    .suggest(Suggester.of(sg -> sg
                            .suggesters("name-suggest", ss -> ss
                                    .prefix(query)
                                    .completion(cs -> cs
                                            .field("name.suggest")
                                            .size(maxResults)
                                            .skipDuplicates(true)
                                    )
                            )
                    ))
            );

            SearchResponse<ProductDocument> response = elasticsearchClient.search(
                    searchRequest,
                    ProductDocument.class
            );

            List<String> suggestions = new ArrayList<>();
            Map<String, List<Suggestion<ProductDocument>>> suggestMap = response.suggest();

            if (suggestMap != null && suggestMap.containsKey("name-suggest")) {
                List<Suggestion<ProductDocument>> suggestionList = suggestMap.get("name-suggest");
                if (!suggestionList.isEmpty()) {
                    Suggestion<ProductDocument> suggestion = suggestionList.get(0);
                    if (suggestion.isCompletion() && suggestion.completion() != null) {
                        suggestions = suggestion.completion().options().stream()
                                .map(CompletionSuggestOption::text)
                                .distinct()
                                .collect(Collectors.toList());
                    }
                }
            }

            log.debug("Found {} suggestions", suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.error("Error getting suggestions", e);
            throw new SearchException("Failed to get suggestions", e);
        }
    }

    private void addFilters(BoolQuery.Builder boolQuery, SearchFilters filters) {
        // Category filter
        if (filters.categories() != null && !filters.categories().isEmpty()) {
            boolQuery.filter(f -> f.terms(t -> t
                    .field("categoryName.keyword")
                    .terms(ts -> ts.value(filters.categories().stream()
                            .map(c -> co.elastic.clients.elasticsearch._types.FieldValue.of(c))
                            .collect(Collectors.toList())))
            ));
        }

        // Price range filter
        if (filters.priceRange() != null) {
            PriceRange priceRange = filters.priceRange();
            RangeQuery.Builder rangeBuilder = new RangeQuery.Builder().field("basePrice");

            if (priceRange.min() != null) {
                rangeBuilder.gte(co.elastic.clients.json.JsonData.of(priceRange.min()));
            }
            if (priceRange.max() != null) {
                rangeBuilder.lte(co.elastic.clients.json.JsonData.of(priceRange.max()));
            }

            boolQuery.filter(f -> f.range(rangeBuilder.build()));
        }

        // Status filter
        if (filters.statuses() != null && !filters.statuses().isEmpty()) {
            boolQuery.filter(f -> f.terms(t -> t
                    .field("status")
                    .terms(ts -> ts.value(filters.statuses().stream()
                            .map(s -> co.elastic.clients.elasticsearch._types.FieldValue.of(s))
                            .collect(Collectors.toList())))
            ));
        }

        // Seller filter
        if (filters.sellerId() != null && !filters.sellerId().isBlank()) {
            boolQuery.filter(f -> f.term(t -> t
                    .field("sellerId")
                    .value(filters.sellerId())
            ));
        }

        // Sizes filter
        if (filters.sizes() != null && !filters.sizes().isEmpty()) {
            boolQuery.filter(f -> f.terms(t -> t
                    .field("availableSizes")
                    .terms(ts -> ts.value(filters.sizes().stream()
                            .map(s -> co.elastic.clients.elasticsearch._types.FieldValue.of(s))
                            .collect(Collectors.toList())))
            ));
        }

        // Colors filter
        if (filters.colors() != null && !filters.colors().isEmpty()) {
            boolQuery.filter(f -> f.terms(t -> t
                    .field("availableColors")
                    .terms(ts -> ts.value(filters.colors().stream()
                            .map(c -> co.elastic.clients.elasticsearch._types.FieldValue.of(c))
                            .collect(Collectors.toList())))
            ));
        }

        // Featured filter
        if (filters.featured() != null) {
            boolQuery.filter(f -> f.term(t -> t
                    .field("featured")
                    .value(filters.featured())
            ));
        }
    }

    private void addSorting(org.springframework.data.elasticsearch.core.query.NativeQuery.NativeQueryBuilder queryBuilder,
                           SortOptions sortOptions) {
        SortOptions.SortField field = sortOptions.field();
        SortOptions.SortDirection direction = sortOptions.direction();
        SortOrder order = direction == SortOptions.SortDirection.ASC ? SortOrder.Asc : SortOrder.Desc;

        switch (field) {
            case PRICE:
                queryBuilder.withSort(s -> s.field(f -> f.field("basePrice").order(order)));
                break;
            case NEWEST:
                queryBuilder.withSort(s -> s.field(f -> f.field("createdAt").order(order)));
                break;
            case RELEVANCE:
            default:
                queryBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
                break;
        }
    }

    private void addAggregations(org.springframework.data.elasticsearch.core.query.NativeQuery.NativeQueryBuilder queryBuilder) {
        // Categories aggregation
        queryBuilder.withAggregation("categories", Aggregation.of(a -> a
                .terms(t -> t.field("categoryName.keyword").size(50))
        ));

        // Price ranges aggregation
        queryBuilder.withAggregation("priceRanges", Aggregation.of(a -> a
                .range(r -> r
                        .field("basePrice")
                        .ranges(
                                AggregationRange.of(ar -> ar.to("25.0").key("Under $25")),
                                AggregationRange.of(ar -> ar.from("25.0").to("50.0").key("$25 - $50")),
                                AggregationRange.of(ar -> ar.from("50.0").to("100.0").key("$50 - $100")),
                                AggregationRange.of(ar -> ar.from("100.0").key("$100 and above"))
                        )
                )
        ));

        // Sizes aggregation
        queryBuilder.withAggregation("sizes", Aggregation.of(a -> a
                .terms(t -> t.field("availableSizes").size(20))
        ));

        // Colors aggregation
        queryBuilder.withAggregation("colors", Aggregation.of(a -> a
                .terms(t -> t.field("availableColors").size(30))
        ));
    }
}
