package com.marketplace.inventory.controller;

import com.marketplace.inventory.dto.CreateStockItemRequest;
import com.marketplace.inventory.dto.UpdateStockRequest;
import com.marketplace.inventory.service.StockService;
import com.marketplace.shared.dto.inventory.StockItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory/admin")
@Tag(name = "Admin Stock Management", description = "Admin APIs for managing inventory stock levels")
public class AdminStockController {

    private static final Logger log = LoggerFactory.getLogger(AdminStockController.class);

    private final StockService stockService;

    public AdminStockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/stock-items")
    @Operation(summary = "Create or update stock item", description = "Create a new stock item or update an existing one")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Stock item created successfully"),
        @ApiResponse(responseCode = "200", description = "Stock item updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<StockItemResponse> createOrUpdateStockItem(
            @Valid @RequestBody CreateStockItemRequest request) {

        log.info("Admin creating/updating stock item for SKU: {}", request.sku());

        StockItemResponse response = stockService.createOrUpdateStockItem(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/stock-items/{sku}")
    @Operation(summary = "Adjust stock quantity", description = "Adjust available quantity for a stock item")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock adjusted successfully"),
        @ApiResponse(responseCode = "404", description = "SKU not found"),
        @ApiResponse(responseCode = "400", description = "Invalid adjustment")
    })
    public ResponseEntity<StockItemResponse> adjustStock(
            @PathVariable String sku,
            @Valid @RequestBody UpdateStockRequest request) {

        log.info("Admin adjusting stock for SKU: {} by delta: {}", sku, request.availableQtyDelta());

        StockItemResponse response = stockService.adjustStock(sku, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock-items/{sku}")
    @Operation(summary = "Get stock item by SKU", description = "Retrieve stock item details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock item found"),
        @ApiResponse(responseCode = "404", description = "SKU not found")
    })
    public ResponseEntity<StockItemResponse> getStockItem(@PathVariable String sku) {

        log.info("Admin fetching stock item: {}", sku);

        StockItemResponse response = stockService.getStockItem(sku);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock-items")
    @Operation(summary = "List all stock items", description = "Get paginated list of all stock items")
    @ApiResponse(responseCode = "200", description = "Stock items retrieved successfully")
    public ResponseEntity<Page<StockItemResponse>> getAllStockItems(
            @PageableDefault(size = 20, sort = "sku", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("Admin fetching all stock items (page: {}, size: {})",
            pageable.getPageNumber(), pageable.getPageSize());

        Page<StockItemResponse> response = stockService.getAllStockItems(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items", description = "Get items below low stock threshold")
    @ApiResponse(responseCode = "200", description = "Low stock items retrieved successfully")
    public ResponseEntity<Page<StockItemResponse>> getLowStockItems(
            @PageableDefault(size = 20, sort = "availableQty", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("Admin fetching low stock items");

        Page<StockItemResponse> response = stockService.getLowStockItems(pageable);

        return ResponseEntity.ok(response);
    }
}
