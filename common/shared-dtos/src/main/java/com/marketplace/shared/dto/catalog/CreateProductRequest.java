package com.marketplace.shared.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Request to create a new product")
public record CreateProductRequest(
    @Schema(description = "Seller ID (only used by admins, otherwise derived from JWT)")
    UUID sellerId,
    
    @NotBlank(message = "Product name is required")
    @Size(max = 500, message = "Product name must not exceed 500 characters")
    @Schema(description = "Product name", example = "Premium Cotton T-Shirt")
    String name,
    
    @NotBlank(message = "Product description is required")
    @Schema(description = "Product description")
    String description,
    
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.00", message = "Price must be positive")
    @Schema(description = "Base price", example = "29.99")
    BigDecimal basePrice,
    
    @NotNull(message = "Category ID is required")
    @Schema(description = "Category ID", example = "1")
    Long categoryId,
    
    @Schema(description = "Available sizes", example = "[\"S\", \"M\", \"L\", \"XL\"]")
    String[] availableSizes,
    
    @Schema(description = "Available colors", example = "[\"Red\", \"Blue\", \"Black\"]")
    String[] availableColors,
    
    @Schema(description = "Stock per variant", example = "{\"S-Red\": 10, \"M-Blue\": 5}")
    Map<String, Integer> stockPerVariant,
    
    @NotEmpty(message = "At least one image URL is required")
    @Schema(description = "Product image URLs")
    String[] imageUrls
) {}
