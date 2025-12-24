package com.marketplace.catalog.controller;

import com.marketplace.catalog.service.CategoryService;
import com.marketplace.shared.dto.catalog.CategoryResponse;
import com.marketplace.shared.dto.catalog.CreateCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Category management endpoints for organizing products hierarchically")
public class CategoryController {
    
    private final CategoryService categoryService;
    
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new category",
            description = "Creates a new product category. Can be a top-level category or a subcategory. Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not authorized (ADMIN role required)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Parent category not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Category with this name already exists", content = @Content)
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }
    
    @GetMapping("/{id}")
    @Operation(
            summary = "Get category by ID",
            description = "Retrieves a specific category by its ID. Public endpoint, no authentication required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<CategoryResponse> getCategory(
            @Parameter(description = "Category ID", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }
    
    @GetMapping
    @Operation(
            summary = "Get all categories",
            description = "Retrieves all categories including top-level and subcategories. Public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
    
    @GetMapping("/top-level")
    @Operation(
            summary = "Get top-level categories",
            description = "Retrieves only top-level categories (categories without a parent). Public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top-level categories retrieved successfully")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<List<CategoryResponse>> getTopLevelCategories() {
        return ResponseEntity.ok(categoryService.getTopLevelCategories());
    }
    
    @GetMapping("/{parentCategoryId}/subcategories")
    @Operation(
            summary = "Get subcategories",
            description = "Retrieves all subcategories of a specific parent category. Public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subcategories retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Parent category not found", content = @Content)
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<List<CategoryResponse>> getSubcategories(
            @Parameter(description = "Parent category ID", required = true, example = "1")
            @PathVariable Long parentCategoryId) {
        return ResponseEntity.ok(categoryService.getSubcategories(parentCategoryId));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a category",
            description = "Deletes a category by its ID. Requires ADMIN role. Cannot delete categories with associated products."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Not authorized (ADMIN role required)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Cannot delete category with products", content = @Content)
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Category ID", required = true, example = "1")
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
