package com.marketplace.inventory.controller;

import com.marketplace.inventory.service.StockService;
import com.marketplace.shared.dto.inventory.StockAvailabilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/public")
@Tag(name = "Public Inventory", description = "Public read-only APIs for checking stock availability")
public class PublicInventoryController {

    private static final Logger log = LoggerFactory.getLogger(PublicInventoryController.class);

    private final StockService stockService;

    public PublicInventoryController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stock/{sku}")
    @Operation(summary = "Check stock availability for a SKU", description = "Public endpoint to check if item is in stock")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock availability retrieved"),
        @ApiResponse(responseCode = "404", description = "SKU not found")
    })
    public ResponseEntity<StockAvailabilityResponse> checkAvailability(@PathVariable String sku) {

        log.debug("Public availability check for SKU: {}", sku);

        StockAvailabilityResponse response = stockService.checkAvailability(sku);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/stock/bulk")
    @Operation(summary = "Check stock availability for multiple SKUs", description = "Bulk availability check")
    @ApiResponse(responseCode = "200", description = "Bulk stock availability retrieved")
    public ResponseEntity<List<StockAvailabilityResponse>> checkBulkAvailability(
            @RequestBody List<String> skus) {

        log.debug("Public bulk availability check for {} SKUs", skus.size());

        List<StockAvailabilityResponse> response = stockService.checkBulkAvailability(skus);

        return ResponseEntity.ok(response);
    }
}
