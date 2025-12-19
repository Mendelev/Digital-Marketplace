package com.marketplace.catalog.service;

import com.marketplace.catalog.domain.model.ProductPriceHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.marketplace.catalog.domain.repository.ProductPriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class PriceHistoryService {
    
    private static final Logger log = LoggerFactory.getLogger(PriceHistoryService.class);
    private final ProductPriceHistoryRepository priceHistoryRepository;
    
    public PriceHistoryService(ProductPriceHistoryRepository priceHistoryRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
    }
    
    public void recordPriceChange(UUID productId, BigDecimal oldPrice, BigDecimal newPrice, UUID changedBy) {
        ProductPriceHistory history = new ProductPriceHistory();
        history.setProductId(productId);
        history.setOldPrice(oldPrice);
        history.setNewPrice(newPrice);
        history.setChangedBy(changedBy);
        
        priceHistoryRepository.save(history);
        log.info("Recorded price change for product {}: {} -> {}", productId, oldPrice, newPrice);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductPriceHistory> getPriceHistory(UUID productId, Pageable pageable) {
        return priceHistoryRepository.findByProductId(productId, pageable);
    }
}
