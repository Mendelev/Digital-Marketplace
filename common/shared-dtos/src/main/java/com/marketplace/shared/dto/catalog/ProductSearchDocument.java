package com.marketplace.shared.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product search document for search service indexing")
public record ProductSearchDocument(
    @Schema(description = "Product ID")
    UUID productId,
    
    @Schema(description = "Product name")
    String name,
    
    @Schema(description = "Product description")
    String description,
    
    @Schema(description = "Base price")
    BigDecimal basePrice,
    
    @Schema(description = "Category name")
    String categoryName,
    
    @Schema(description = "Seller ID")
    UUID sellerId,
    
    @Schema(description = "Product status")
    String status,
    
    @Schema(description = "Available sizes")
    String[] availableSizes,
    
    @Schema(description = "Available colors")
    String[] availableColors,
    
    @Schema(description = "Thumbnail image URL")
    String thumbnailUrl
) {}
