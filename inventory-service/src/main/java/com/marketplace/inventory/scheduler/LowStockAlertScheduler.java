package com.marketplace.inventory.scheduler;

import com.marketplace.inventory.domain.model.StockItem;
import com.marketplace.inventory.domain.repository.StockItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled job to monitor low stock levels.
 * Runs every 30 minutes to check for items below low stock threshold.
 */
@Component
public class LowStockAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(LowStockAlertScheduler.class);

    private final StockItemRepository stockItemRepository;

    public LowStockAlertScheduler(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    @Transactional(readOnly = true)
    public void checkLowStock() {
        log.debug("Starting low stock check");

        List<StockItem> lowStockItems = stockItemRepository.findLowStockItems();

        if (lowStockItems.isEmpty()) {
            log.debug("No low stock items found");
            return;
        }

        log.warn("Found {} items with low stock", lowStockItems.size());

        for (StockItem item : lowStockItems) {
            log.warn("Low stock alert - SKU: {}, Available: {}, Threshold: {}",
                item.getSku(),
                item.getAvailableQty(),
                item.getLowStockThreshold());
        }

        // In production, this would publish events to Notification Service
        // For now, we just log the alerts
    }
}
