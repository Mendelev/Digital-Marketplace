package com.marketplace.search.controller;

import com.marketplace.search.document.ProductDocument;
import com.marketplace.search.service.IndexingService;
import com.marketplace.shared.dto.catalog.ProductSearchDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search/index")
@Tag(name = "Search Index", description = "Index management endpoints for product documents")
public class SearchIndexController {

    private final IndexingService indexingService;

    public SearchIndexController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @PostMapping("/product")
    @Operation(summary = "Index product", description = "Index a product document in Elasticsearch")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product indexed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Indexing failed")
    })
    public ResponseEntity<Void> indexProduct(@RequestBody ProductSearchDocument request) {
        indexingService.indexProduct(toDocument(request));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/product/{productId}")
    @Operation(summary = "Update product index", description = "Update a product document in Elasticsearch")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Update failed")
    })
    public ResponseEntity<Void> updateProduct(@PathVariable String productId,
                                              @RequestBody ProductSearchDocument request) {
        ProductSearchDocument normalized = new ProductSearchDocument(
            request.productId() != null ? request.productId() : java.util.UUID.fromString(productId),
            request.name(),
            request.description(),
            request.basePrice(),
            request.categoryName(),
            request.sellerId(),
            request.status(),
            request.availableSizes(),
            request.availableColors(),
            request.thumbnailUrl()
        );
        indexingService.updateProduct(toDocument(normalized));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/product/{productId}")
    @Operation(summary = "Delete product index", description = "Remove a product document from Elasticsearch")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "500", description = "Delete failed")
    })
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {
        indexingService.deleteProduct(productId);
        return ResponseEntity.ok().build();
    }

    private ProductDocument toDocument(ProductSearchDocument request) {
        List<String> sizes = request.availableSizes() == null
            ? null
            : Arrays.asList(request.availableSizes());
        List<String> colors = request.availableColors() == null
            ? null
            : Arrays.asList(request.availableColors());

        ProductDocument document = new ProductDocument();
        document.setProductId(request.productId().toString());
        document.setName(request.name());
        document.setDescription(request.description());
        document.setBasePrice(request.basePrice());
        document.setCategoryName(request.categoryName());
        document.setSellerId(request.sellerId() != null ? request.sellerId().toString() : null);
        document.setStatus(request.status());
        document.setAvailableSizes(sizes);
        document.setAvailableColors(colors);
        document.setThumbnailUrl(request.thumbnailUrl());
        return document;
    }
}
