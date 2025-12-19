package com.marketplace.order.service;

import com.marketplace.order.config.OrderServiceProperties;
import com.marketplace.order.domain.model.*;
import com.marketplace.order.domain.repository.ReservationRepository;
import com.marketplace.order.domain.repository.StockItemRepository;
import com.marketplace.order.dto.ReservationResponse;
import com.marketplace.order.exception.InsufficientStockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mock inventory service for simulating inventory reservation.
 * In production, this would integrate with a real inventory management system.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final int DEFAULT_STOCK_QUANTITY = 100;

    private final StockItemRepository stockItemRepository;
    private final ReservationRepository reservationRepository;
    private final OrderServiceProperties properties;

    public InventoryService(StockItemRepository stockItemRepository,
                            ReservationRepository reservationRepository,
                            OrderServiceProperties properties) {
        this.stockItemRepository = stockItemRepository;
        this.reservationRepository = reservationRepository;
        this.properties = properties;
    }

    /**
     * Reserve stock for order items.
     * Creates a reservation with TTL and updates stock quantities.
     *
     * @param orderId Order ID
     * @param items   List of order items to reserve
     * @return ReservationResponse
     * @throws InsufficientStockException if stock is insufficient
     */
    @Transactional
    public ReservationResponse reserveStock(UUID orderId, List<OrderItem> items) {
        log.info("Reserving stock for order: {}", orderId);

        // Check if stock is available for all items
        for (OrderItem item : items) {
            StockItem stock = stockItemRepository.findById(item.getSku())
                    .orElseGet(() -> createDefaultStock(item.getSku(), item.getProductId()));

            if (stock.getAvailableQty() < item.getQuantity()) {
                log.warn("Insufficient stock for SKU: {}. Available: {}, Requested: {}",
                        item.getSku(), stock.getAvailableQty(), item.getQuantity());
                throw new InsufficientStockException(
                        String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                                item.getSku(), stock.getAvailableQty(), item.getQuantity())
                );
            }
        }

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setOrderId(orderId);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(OffsetDateTime.now().plusMinutes(properties.reservationTtlMinutes()));

        // Add reservation lines and update stock
        for (OrderItem item : items) {
            ReservationLine line = new ReservationLine(item.getSku(), item.getQuantity());
            reservation.addLine(line);

            // Update stock quantities
            StockItem stock = stockItemRepository.findById(item.getSku()).orElseThrow();
            stock.setAvailableQty(stock.getAvailableQty() - item.getQuantity());
            stock.setReservedQty(stock.getReservedQty() + item.getQuantity());
            stockItemRepository.save(stock);

            log.debug("Reserved {} units of SKU: {}. New available: {}, reserved: {}",
                    item.getQuantity(), item.getSku(), stock.getAvailableQty(), stock.getReservedQty());
        }

        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("Stock reserved successfully. Reservation ID: {}, Expires at: {}",
                savedReservation.getReservationId(), savedReservation.getExpiresAt());

        return new ReservationResponse(
                savedReservation.getReservationId(),
                true,
                "Stock reserved successfully"
        );
    }

    /**
     * Confirm reservation - commit stock to order.
     * Decrements reserved quantity (stock is now committed to order).
     */
    @Transactional
    public void confirmReservation(UUID reservationId) {
        log.info("Confirming reservation: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Cannot confirm reservation in status: " + reservation.getStatus());
        }

        // Update stock: decrement reserved quantity (stock is committed)
        for (ReservationLine line : reservation.getLines()) {
            StockItem stock = stockItemRepository.findById(line.getSku()).orElseThrow();
            stock.setReservedQty(stock.getReservedQty() - line.getQuantity());
            stockItemRepository.save(stock);

            log.debug("Confirmed {} units of SKU: {}. Reserved qty: {}",
                    line.getQuantity(), line.getSku(), stock.getReservedQty());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        log.info("Reservation confirmed: {}", reservationId);
    }

    /**
     * Release reservation - return stock to available inventory.
     * Used when order is cancelled or payment fails.
     */
    @Transactional
    public void releaseReservation(UUID orderId) {
        log.info("Releasing reservation for order: {}", orderId);

        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElse(null);

        if (reservation == null) {
            log.warn("No reservation found for order: {}", orderId);
            return;
        }

        if (reservation.getStatus() == ReservationStatus.RELEASED ||
                reservation.getStatus() == ReservationStatus.CONFIRMED) {
            log.warn("Reservation already in final state: {}", reservation.getStatus());
            return;
        }

        // Return reserved stock to available
        for (ReservationLine line : reservation.getLines()) {
            StockItem stock = stockItemRepository.findById(line.getSku()).orElseThrow();
            stock.setAvailableQty(stock.getAvailableQty() + line.getQuantity());
            stock.setReservedQty(stock.getReservedQty() - line.getQuantity());
            stockItemRepository.save(stock);

            log.debug("Released {} units of SKU: {}. Available: {}, Reserved: {}",
                    line.getQuantity(), line.getSku(), stock.getAvailableQty(), stock.getReservedQty());
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        log.info("Reservation released for order: {}", orderId);
    }

    /**
     * Create default stock for a SKU (mock behavior).
     * In production, this would be managed separately.
     */
    private StockItem createDefaultStock(String sku, UUID productId) {
        log.info("Creating default stock for SKU: {} with {} units", sku, DEFAULT_STOCK_QUANTITY);

        StockItem stock = new StockItem(sku, productId, DEFAULT_STOCK_QUANTITY);
        return stockItemRepository.save(stock);
    }
}
