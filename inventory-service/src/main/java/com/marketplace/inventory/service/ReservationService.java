package com.marketplace.inventory.service;

import com.marketplace.inventory.config.InventoryServiceProperties;
import com.marketplace.inventory.domain.model.*;
import com.marketplace.inventory.domain.repository.ReservationRepository;
import com.marketplace.inventory.domain.repository.StockItemRepository;
import com.marketplace.inventory.exception.DuplicateReservationException;
import com.marketplace.inventory.exception.InvalidSKUException;
import com.marketplace.inventory.exception.ReservationNotFoundException;
import com.marketplace.shared.dto.inventory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final StockItemRepository stockItemRepository;
    private final StockMovementService stockMovementService;
    private final StockService stockService;
    private final InventoryServiceProperties properties;

    public ReservationService(ReservationRepository reservationRepository,
                             StockItemRepository stockItemRepository,
                             StockMovementService stockMovementService,
                             StockService stockService,
                             InventoryServiceProperties properties) {
        this.reservationRepository = reservationRepository;
        this.stockItemRepository = stockItemRepository;
        this.stockMovementService = stockMovementService;
        this.stockService = stockService;
        this.properties = properties;
    }

    @Transactional
    public ReservationResponse reserveStock(ReserveStockRequest request) {
        MDC.put("operation", "reserveStock");
        MDC.put("orderId", request.orderId().toString());

        log.info("Reserving stock for order: {}", request.orderId());

        // 1. Check for duplicate reservation
        if (reservationRepository.existsByOrderId(request.orderId())) {
            throw new DuplicateReservationException(request.orderId());
        }

        // 2. Create reservation
        OffsetDateTime expiresAt = calculateExpirationTime();
        Reservation reservation = new Reservation(request.orderId(), expiresAt);

        // 3. Reserve stock for each line with pessimistic locking
        for (ReservationLineRequest lineReq : request.lines()) {
            MDC.put("sku", lineReq.sku());

            log.debug("Reserving {} units of SKU: {}", lineReq.quantity(), lineReq.sku());

            // Use pessimistic locking to prevent concurrent modifications
            StockItem stockItem = stockItemRepository.findBySkuForUpdate(lineReq.sku())
                    .orElseThrow(() -> {
                        log.error("SKU not found: {}", lineReq.sku());
                        return new InvalidSKUException(lineReq.sku());
                    });

            // Check and reserve stock (throws InsufficientStockException if not enough)
            int prevAvailable = stockItem.getAvailableQty();
            int prevReserved = stockItem.getReservedQty();

            stockItem.reserve(lineReq.quantity());
            stockItemRepository.save(stockItem);

            log.info("Reserved {} units of SKU: {} (available: {} -> {}, reserved: {} -> {})",
                lineReq.quantity(), lineReq.sku(),
                prevAvailable, stockItem.getAvailableQty(),
                prevReserved, stockItem.getReservedQty());

            // Create reservation line
            ReservationLine line = new ReservationLine(lineReq.sku(), lineReq.quantity());
            reservation.addLine(line);

            // Record movement
            stockMovementService.recordMovement(
                stockItem,
                MovementType.RESERVE,
                lineReq.quantity(),
                null, // Will set after saving reservation
                "System",
                "Stock reserved for order " + request.orderId()
            );

            MDC.remove("sku");
        }

        // 4. Save reservation
        reservation = reservationRepository.save(reservation);
        MDC.put("reservationId", reservation.getReservationId().toString());

        log.info("Stock reservation created: {} for order: {} (expires at: {})",
            reservation.getReservationId(), request.orderId(), expiresAt);

        return toResponse(reservation);
    }

    @Transactional
    public void confirmReservation(UUID reservationId) {
        MDC.put("operation", "confirmReservation");
        MDC.put("reservationId", reservationId.toString());

        log.info("Confirming reservation: {}", reservationId);

        Reservation reservation = findReservationOrThrow(reservationId);
        MDC.put("orderId", reservation.getOrderId().toString());

        // Validate and confirm (throws exceptions if invalid state)
        reservation.confirm();
        reservationRepository.save(reservation);

        // Record movements for audit (no qty change, just status change)
        for (ReservationLine line : reservation.getLines()) {
            StockItem stockItem = stockService.findStockItemOrThrow(line.getSku());
            stockMovementService.recordMovement(
                stockItem,
                MovementType.CONFIRM,
                0,
                reservationId,
                "System",
                "Reservation confirmed for order " + reservation.getOrderId()
            );
        }

        log.info("Reservation confirmed: {} for order: {}", reservationId, reservation.getOrderId());
    }

    @Transactional
    public void releaseReservation(UUID reservationId, String reason) {
        MDC.put("operation", "releaseReservation");
        MDC.put("reservationId", reservationId.toString());

        log.info("Releasing reservation: {} (reason: {})", reservationId, reason);

        Reservation reservation = findReservationOrThrow(reservationId);
        MDC.put("orderId", reservation.getOrderId().toString());

        // Return stock to available for each line
        for (ReservationLine line : reservation.getLines()) {
            StockItem stockItem = stockItemRepository.findBySkuForUpdate(line.getSku())
                    .orElseThrow(() -> new InvalidSKUException(line.getSku()));

            int prevAvailable = stockItem.getAvailableQty();
            int prevReserved = stockItem.getReservedQty();

            stockItem.releaseReservation(line.getQuantity());
            stockItemRepository.save(stockItem);

            log.info("Released {} units of SKU: {} (available: {} -> {}, reserved: {} -> {})",
                line.getQuantity(), line.getSku(),
                prevAvailable, stockItem.getAvailableQty(),
                prevReserved, stockItem.getReservedQty());

            // Record movement
            stockMovementService.recordMovement(
                stockItem,
                MovementType.RELEASE,
                line.getQuantity(),
                reservationId,
                "System",
                reason
            );
        }

        // Update reservation status
        reservation.release();
        reservationRepository.save(reservation);

        log.info("Reservation released: {} for order: {}", reservationId, reservation.getOrderId());
    }

    @Transactional
    public void expireReservation(UUID reservationId) {
        MDC.put("operation", "expireReservation");
        MDC.put("reservationId", reservationId.toString());

        log.info("Expiring reservation: {}", reservationId);

        Reservation reservation = findReservationOrThrow(reservationId);

        // Release stock (same as release but sets status to EXPIRED)
        for (ReservationLine line : reservation.getLines()) {
            StockItem stockItem = stockItemRepository.findBySkuForUpdate(line.getSku())
                    .orElseThrow(() -> new InvalidSKUException(line.getSku()));

            stockItem.releaseReservation(line.getQuantity());
            stockItemRepository.save(stockItem);

            log.debug("Released {} units of SKU: {} due to expiration",
                line.getQuantity(), line.getSku());

            // Record movement
            stockMovementService.recordMovement(
                stockItem,
                MovementType.RELEASE,
                line.getQuantity(),
                reservationId,
                "System",
                "Reservation expired"
            );
        }

        // Mark as expired
        reservation.expire();
        reservationRepository.save(reservation);

        log.info("Reservation expired: {} for order: {}", reservationId, reservation.getOrderId());
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(UUID reservationId) {
        MDC.put("operation", "getReservation");
        MDC.put("reservationId", reservationId.toString());

        Reservation reservation = findReservationOrThrow(reservationId);
        return toResponse(reservation);
    }

    // Helper methods

    private OffsetDateTime calculateExpirationTime() {
        return OffsetDateTime.now().plusMinutes(properties.reservationTtlMinutes());
    }

    private Reservation findReservationOrThrow(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        List<ReservationLineResponse> lines = reservation.getLines().stream()
                .map(line -> new ReservationLineResponse(
                    line.getReservationLineId(),
                    line.getSku(),
                    line.getQuantity()
                ))
                .toList();

        return new ReservationResponse(
            reservation.getReservationId(),
            reservation.getOrderId(),
            reservation.getStatus().name(),
            reservation.getExpiresAt(),
            lines,
            reservation.getCreatedAt()
        );
    }
}
