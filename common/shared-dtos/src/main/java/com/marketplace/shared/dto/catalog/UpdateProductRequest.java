package com.marketplace.shared.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Request to update an existing product")
public record UpdateProductRequest(
    @Size(max = 500, message = "Product name must not exceed 500 characters")
    @Schema(description = "Product name")
    String name,
    
    @Schema(description = "Product description")
    String description,
    
    @DecimalMin(value = "0.00", message = "Price must be positive")
    @Schema(description = "Base price")
    BigDecimal basePrice,
    
    @Schema(description = "Available sizes")
    String[] availableSizes,
    
    @Schema(description = "Available colors")
    String[] availableColors,
    
    @Schema(description = "Stock per variant")
    Map<String, Integer> stockPerVariant,
    
    @Schema(description = "Product image URLs")
    String[] imageUrls,
    
    @Schema(description = "Product status")
    String status
) {}
