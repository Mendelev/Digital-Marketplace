package com.marketplace.catalog.controller;

import com.marketplace.catalog.domain.enums.ProductStatus;
import com.marketplace.catalog.security.UserPrincipal;
import com.marketplace.catalog.service.ProductService;
import com.marketplace.shared.dto.catalog.CreateProductRequest;
import com.marketplace.shared.dto.catalog.ProductResponse;
import com.marketplace.shared.dto.catalog.UpdateProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product management endpoints for catalog operations with multi-seller support")
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @Operation(summary = "Create product", description = "Creates a new product. SELLER role uses their own ID, ADMIN can specify any seller ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Requires SELLER or ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @SecurityRequirement(name = "basicAuth")
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        // Seller can only create products for themselves
        UUID effectiveSellerId = principal.hasRole("ADMIN") && request.sellerId() != null
            ? request.sellerId() 
            : principal.getUserId();
        
        ProductResponse product = productService.createProduct(request, effectiveSellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    @Operation(summary = "Update product", description = "Updates an existing product. Only the product owner or ADMIN can update.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this product"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "basicAuth")
    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        // Verify ownership before update
        productService.verifyOwnership(productId, principal.getUserId(), principal.hasRole("ADMIN"));
        
        ProductResponse product = productService.updateProduct(productId, request);
        return ResponseEntity.ok(product);
    }
    
    @Operation(summary = "Get product by ID", description = "Retrieves a single product by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "basicAuth")
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @Parameter(description = "Product ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }
    
    @Operation(summary = "List products", description = "Retrieves a paginated list of products with optional filters for category, seller, and status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    @SecurityRequirement(name = "basicAuth")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> listProducts(
            @Parameter(description = "Filter by category ID", example = "1")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by seller ID", example = "650e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Filter by product status", example = "ACTIVE")
            @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (field,direction)", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSortOrders(sort)));
        return ResponseEntity.ok(productService.listProducts(categoryId, sellerId, status, pageable));
    }
    
    @Operation(summary = "Get my products", description = "Retrieves all products owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Requires SELLER role")
    })
    @SecurityRequirement(name = "basicAuth")
    @GetMapping("/seller/my-products")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (field,direction)", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSortOrders(sort)));
        return ResponseEntity.ok(productService.getProductsBySeller(principal.getUserId(), pageable));
    }
    
    @Operation(summary = "Get featured products", description = "Retrieves a paginated list of featured products")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Featured products retrieved successfully")
    })
    @SecurityRequirement(name = "basicAuth")
    @GetMapping("/featured")
    public ResponseEntity<Page<ProductResponse>> getFeaturedProducts(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getFeaturedProducts(pageable));
    }
    
    @Operation(summary = "Delete product", description = "Deletes a product. Only the product owner or ADMIN can delete.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this product"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "basicAuth")
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        productService.verifyOwnership(productId, principal.getUserId(), principal.hasRole("ADMIN"));
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
    
    private List<Sort.Order> parseSortOrders(String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();
        for (String sortStr : sort) {
            String[] parts = sortStr.split(",");
            String property = parts[0];
            Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
            orders.add(new Sort.Order(direction, property));
        }
        return orders;
    }
}
