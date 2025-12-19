package com.marketplace.shared.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new category")
public record CreateCategoryRequest(
    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    @Schema(description = "Category name", example = "Electronics")
    String name,
    
    @Schema(description = "Category description", example = "Electronic devices and accessories")
    String description,
    
    @Schema(description = "Parent category ID for subcategories. Leave empty or null for top-level categories.", 
            example = "null", nullable = true)
    Long parentCategoryId
) {}
