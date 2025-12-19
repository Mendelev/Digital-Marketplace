package com.marketplace.inventory.service;

import com.marketplace.inventory.domain.model.MovementType;
import com.marketplace.inventory.domain.model.StockItem;
import com.marketplace.inventory.domain.model.StockMovement;
import com.marketplace.inventory.domain.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StockMovementService {

    private final StockMovementRepository repository;

    public StockMovementService(StockMovementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordMovement(StockItem stockItem, MovementType type, Integer quantity,
                              UUID reservationId, String createdBy, String reason) {
        StockMovement movement = new StockMovement(
            stockItem.getSku(),
            type,
            quantity,
            stockItem.getAvailableQty(),
            stockItem.getAvailableQty(),
            stockItem.getReservedQty(),
            stockItem.getReservedQty(),
            reservationId,
            reason,
            createdBy
        );

        repository.save(movement);
    }
}
