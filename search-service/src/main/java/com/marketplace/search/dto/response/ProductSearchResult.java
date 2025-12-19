package com.marketplace.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Product search result with relevance score.
 */
@Schema(description = "Product search result with relevance score and complete product information")
public record ProductSearchResult(
        @Schema(description = "Product ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String productId,

        @Schema(description = "Product name", example = "Dell XPS 15 Laptop")
        String name,

        @Schema(description = "Product description", example = "15-inch laptop with Intel i7 processor, 16GB RAM, and 512GB SSD")
        String description,

        @Schema(description = "Base price in USD", example = "1299.99")
        BigDecimal basePrice,

        @Schema(description = "Category name", example = "Electronics")
        String categoryName,

        @Schema(description = "Seller ID", example = "660e8400-e29b-41d4-a716-446655440001")
        String sellerId,

        @Schema(description = "Product status", example = "ACTIVE")
        String status,

        @Schema(description = "Available sizes", example = "[\"M\", \"L\", \"XL\"]")
        List<String> availableSizes,

        @Schema(description = "Available colors", example = "[\"Black\", \"Silver\"]")
        List<String> availableColors,

        @Schema(description = "Thumbnail image URL", example = "https://example.com/images/laptop-thumb.jpg")
        String thumbnailUrl,

        @Schema(description = "Featured product flag", example = "true")
        Boolean featured,

        @Schema(description = "Product creation timestamp", example = "2024-01-15T10:30:00Z")
        OffsetDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2024-01-20T15:45:00Z")
        OffsetDateTime updatedAt,

        @Schema(description = "Search relevance score (0.0 to 1.0)", example = "0.95")
        Float score
) {}
