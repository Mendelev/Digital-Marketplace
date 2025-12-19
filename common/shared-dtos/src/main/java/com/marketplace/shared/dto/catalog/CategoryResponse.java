package com.marketplace.shared.dto.catalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Category details response")
public record CategoryResponse(
    @Schema(description = "Category unique identifier")
    Long id,
    
    @Schema(description = "Category name")
    String name,
    
    @Schema(description = "Category description")
    String description,
    
    @Schema(description = "Parent category ID")
    Long parentCategoryId,
    
    @Schema(description = "Parent category name")
    String parentCategoryName,
    
    @Schema(description = "Creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime createdAt,
    
    @Schema(description = "Last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime updatedAt
) {}
