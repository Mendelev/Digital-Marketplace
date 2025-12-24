package com.marketplace.search.controller;

import com.marketplace.search.dto.request.SearchRequest;
import com.marketplace.search.dto.response.SearchResponse;
import com.marketplace.search.dto.response.SuggestionResponse;
import com.marketplace.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for product search operations.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Product Search", description = "APIs for searching and discovering products")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Search products with filters, sorting, and pagination.
     */
    @PostMapping("/products")
    @Operation(
        summary = "Search products",
        description = """
            Search for products using full-text search with optional filters.

            Supports:
            - Keyword search across product names and descriptions
            - Filtering by category, price range, seller, and availability
            - Multiple sort options (relevance, price, recency)
            - Pagination with configurable page size
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "500", description = "Search service error")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<SearchResponse> searchProducts(
            @Parameter(description = "Search request with query, filters, sort, and pagination")
            @Valid @RequestBody SearchRequest request) {

        log.debug("Search request received: {}", request);

        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get autocomplete suggestions.
     */
    @GetMapping("/suggestions")
    @Operation(
        summary = "Get autocomplete suggestions",
        description = """
            Get product name suggestions based on partial input for autocomplete functionality.

            Returns a list of suggested product names that match the query prefix.
            Useful for implementing search-as-you-type features.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suggestions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "500", description = "Search service error")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<SuggestionResponse> getSuggestions(
            @Parameter(description = "Search query for suggestions (minimum 2 characters)", required = true, example = "lap")
            @RequestParam("q") String query) {

        log.debug("Suggestions request for query: {}", query);

        List<String> suggestions = searchService.getSuggestions(query);
        SuggestionResponse response = new SuggestionResponse(suggestions, suggestions.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if search service is running and responsive"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service is healthy"),
            @ApiResponse(responseCode = "503", description = "Service is unavailable")
    })
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "search-service"
        ));
    }
}
