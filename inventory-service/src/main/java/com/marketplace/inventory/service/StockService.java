package com.marketplace.inventory.service;

import com.marketplace.inventory.domain.model.MovementType;
import com.marketplace.inventory.domain.model.StockItem;
import com.marketplace.inventory.domain.repository.StockItemRepository;
import com.marketplace.inventory.dto.CreateStockItemRequest;
import com.marketplace.inventory.dto.UpdateStockRequest;
import com.marketplace.inventory.exception.InvalidSKUException;
import com.marketplace.shared.dto.inventory.StockAvailabilityResponse;
import com.marketplace.shared.dto.inventory.StockItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockItemRepository stockItemRepository;
    private final StockMovementService stockMovementService;

    public StockService(StockItemRepository stockItemRepository,
                       StockMovementService stockMovementService) {
        this.stockItemRepository = stockItemRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public StockItemResponse createOrUpdateStockItem(CreateStockItemRequest request) {
        MDC.put("operation", "createOrUpdateStockItem");
        MDC.put("sku", request.sku());

        log.info("Creating or updating stock item for SKU: {}", request.sku());

        StockItem stockItem = stockItemRepository.findById(request.sku())
                .orElse(new StockItem(
                    request.sku(),
                    request.productId(),
                    request.initialQty(),
                    request.lowStockThreshold()
                ));

        // If item exists, update it
        if (stockItem.getCreatedAt() != null) {
            int previousQty = stockItem.getAvailableQty();
            stockItem.setProductId(request.productId());
            stockItem.setAvailableQty(request.initialQty());
            stockItem.setLowStockThreshold(request.lowStockThreshold());

            log.info("Updated existing stock item: {} (qty: {} -> {})",
                request.sku(), previousQty, request.initialQty());
        } else {
            log.info("Created new stock item: {} with qty: {}", request.sku(), request.initialQty());
        }

        stockItem = stockItemRepository.save(stockItem);

        // Record movement
        stockMovementService.recordMovement(
            stockItem,
            MovementType.RESTOCK,
            request.initialQty(),
            null,
            "Admin",
            "Initial stock setup"
        );

        return toResponse(stockItem);
    }

    @Transactional
    public StockItemResponse adjustStock(String sku, UpdateStockRequest request) {
        MDC.put("operation", "adjustStock");
        MDC.put("sku", sku);

        log.info("Adjusting stock for SKU: {} by delta: {}", sku, request.availableQtyDelta());

        StockItem stockItem = findStockItemOrThrow(sku);

        int previousQty = stockItem.getAvailableQty();
        int newQty = previousQty + request.availableQtyDelta();

        if (newQty < 0) {
            throw new IllegalArgumentException(
                "Cannot adjust stock: resulting quantity would be negative"
            );
        }

        stockItem.setAvailableQty(newQty);
        stockItem = stockItemRepository.save(stockItem);

        log.info("Stock adjusted for SKU: {} from {} to {}", sku, previousQty, newQty);

        // Record movement
        stockMovementService.recordMovement(
            stockItem,
            MovementType.ADJUSTMENT,
            request.availableQtyDelta(),
            null,
            "Admin",
            request.reason()
        );

        // Check low stock
        if (stockItem.isLowStock()) {
            log.warn("Low stock alert for SKU: {} (available: {}, threshold: {})",
                sku, stockItem.getAvailableQty(), stockItem.getLowStockThreshold());
        }

        return toResponse(stockItem);
    }

    @Transactional(readOnly = true)
    public StockItemResponse getStockItem(String sku) {
        MDC.put("operation", "getStockItem");
        MDC.put("sku", sku);

        StockItem stockItem = findStockItemOrThrow(sku);
        return toResponse(stockItem);
    }

    @Transactional(readOnly = true)
    public Page<StockItemResponse> getAllStockItems(Pageable pageable) {
        MDC.put("operation", "getAllStockItems");

        return stockItemRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StockItemResponse> getLowStockItems(Pageable pageable) {
        MDC.put("operation", "getLowStockItems");

        log.info("Fetching low stock items");
        return stockItemRepository.findLowStockItems(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StockAvailabilityResponse checkAvailability(String sku) {
        MDC.put("operation", "checkAvailability");
        MDC.put("sku", sku);

        StockItem stockItem = findStockItemOrThrow(sku);

        return new StockAvailabilityResponse(
            stockItem.getSku(),
            stockItem.getProductId(),
            stockItem.getAvailableQty(),
            stockItem.getAvailableQty() > 0,
            stockItem.isLowStock()
        );
    }

    @Transactional(readOnly = true)
    public List<StockAvailabilityResponse> checkBulkAvailability(List<String> skus) {
        MDC.put("operation", "checkBulkAvailability");

        log.info("Checking availability for {} SKUs", skus.size());

        return skus.stream()
                .map(sku -> {
                    try {
                        return checkAvailability(sku);
                    } catch (InvalidSKUException e) {
                        // Return unavailable for invalid SKUs
                        return new StockAvailabilityResponse(sku, null, 0, false, false);
                    }
                })
                .toList();
    }

    // Helper methods

    StockItem findStockItemOrThrow(String sku) {
        return stockItemRepository.findById(sku)
                .orElseThrow(() -> new InvalidSKUException(sku));
    }

    private StockItemResponse toResponse(StockItem stockItem) {
        return new StockItemResponse(
            stockItem.getSku(),
            stockItem.getProductId(),
            stockItem.getAvailableQty(),
            stockItem.getReservedQty(),
            stockItem.getTotalQty(),
            stockItem.getLowStockThreshold(),
            stockItem.isLowStock(),
            stockItem.getCreatedAt(),
            stockItem.getUpdatedAt()
        );
    }
}
