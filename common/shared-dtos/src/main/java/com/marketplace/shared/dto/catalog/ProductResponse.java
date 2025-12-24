package com.marketplace.shared.dto.catalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Product details response")
public record ProductResponse(
    @Schema(description = "Product unique identifier")
    UUID id,
    
    @Schema(description = "Seller unique identifier")
    UUID sellerId,
    
    @Schema(description = "Product name")
    String name,
    
    @Schema(description = "Product description")
    String description,
    
    @Schema(description = "Base price")
    BigDecimal basePrice,
    
    @Schema(description = "Category ID")
    Long categoryId,
    
    @Schema(description = "Category name")
    String categoryName,
    
    @Schema(description = "Available sizes")
    String[] availableSizes,
    
    @Schema(description = "Available colors")
    String[] availableColors,
    
    @Schema(description = "Stock per variant (e.g., 'S-Red' -> 10)")
    Map<String, Integer> stockPerVariant,
    
    @Schema(description = "Product image URLs")
    String[] imageUrls,
    
    @Schema(description = "Product status")
    String status,
    
    @Schema(description = "Featured product flag")
    boolean featured,
    
    @Schema(description = "Creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime createdAt,
    
    @Schema(description = "Last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime updatedAt
) {}
