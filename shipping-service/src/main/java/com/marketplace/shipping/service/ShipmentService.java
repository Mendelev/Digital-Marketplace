package com.marketplace.shipping.service;

import com.marketplace.shipping.config.ShippingConfig;
import com.marketplace.shipping.domain.model.AddressSnapshot;
import com.marketplace.shipping.domain.model.Shipment;
import com.marketplace.shipping.domain.model.ShipmentStatus;
import com.marketplace.shipping.domain.repository.ShipmentRepository;
import com.marketplace.shipping.dto.CreateShipmentRequest;
import com.marketplace.shipping.exception.ShipmentAlreadyExistsException;
import com.marketplace.shipping.exception.ShipmentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Core service for shipment management.
 */
@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingService trackingService;
    private final ShippingConfig shippingConfig;

    public ShipmentService(ShipmentRepository shipmentRepository,
                          ShipmentTrackingService trackingService,
                          ShippingConfig shippingConfig) {
        this.shipmentRepository = shipmentRepository;
        this.trackingService = trackingService;
        this.shippingConfig = shippingConfig;
    }

    /**
     * Create a new shipment.
     */
    @Transactional
    public Shipment createShipment(CreateShipmentRequest request) {
        MDC.put("operation", "createShipment");
        MDC.put("orderId", request.orderId().toString());
        MDC.put("userId", request.userId().toString());

        log.info("Creating shipment for order: {}", request.orderId());

        // Check if shipment already exists for this order
        shipmentRepository.findByOrderId(request.orderId()).ifPresent(existing -> {
            log.warn("Shipment already exists for order: {}", request.orderId());
            throw new ShipmentAlreadyExistsException(
                    "Shipment already exists for order: " + request.orderId()
            );
        });

        // Calculate shipping fee
        BigDecimal shippingFee = calculateShippingFee();

        // Create shipment entity
        Shipment shipment = new Shipment(
                request.orderId(),
                request.userId(),
                request.shippingAddress(),
                request.itemCount(),
                shippingFee,
                shippingConfig.defaultCurrency()
        );

        // Set optional fields
        if (request.packageWeightKg() != null) {
            shipment.setPackageWeightKg(request.packageWeightKg());
        }
        if (request.packageDimensions() != null) {
            shipment.setPackageDimensions(request.packageDimensions());
        }

        // Mark as created (generates tracking number and sets estimated delivery)
        shipment.markAsCreated();

        // Save shipment
        Shipment savedShipment = shipmentRepository.save(shipment);
        MDC.put("shipmentId", savedShipment.getShipmentId().toString());

        log.info("Shipment created successfully: shipmentId={}, trackingNumber={}",
                savedShipment.getShipmentId(), savedShipment.getTrackingNumber());

        // Add initial tracking event
        String location = trackingService.generateLocationForStatus(ShipmentStatus.CREATED);
        String description = trackingService.generateDescriptionForStatus(ShipmentStatus.CREATED);
        trackingService.addTrackingEvent(
                savedShipment.getShipmentId(),
                ShipmentStatus.CREATED,
                location,
                description
        );

        return savedShipment;
    }

    /**
     * Get shipment by ID.
     */
    @Transactional(readOnly = true)
    public Shipment getShipment(UUID shipmentId) {
        MDC.put("shipmentId", shipmentId.toString());
        log.debug("Fetching shipment: {}", shipmentId);

        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found: " + shipmentId
                ));
    }

    /**
     * Get shipment by order ID.
     */
    @Transactional(readOnly = true)
    public Shipment getShipmentByOrderId(UUID orderId) {
        MDC.put("orderId", orderId.toString());
        log.debug("Fetching shipment for order: {}", orderId);

        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found for order: " + orderId
                ));
    }

    /**
     * Get shipment by tracking number.
     */
    @Transactional(readOnly = true)
    public Shipment getShipmentByTrackingNumber(String trackingNumber) {
        MDC.put("trackingNumber", trackingNumber);
        log.debug("Fetching shipment by tracking number: {}", trackingNumber);

        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found for tracking number: " + trackingNumber
                ));
    }

    /**
     * Get all shipments for a user.
     */
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByUser(UUID userId) {
        MDC.put("userId", userId.toString());
        log.debug("Fetching shipments for user: {}", userId);

        return shipmentRepository.findByUserId(userId);
    }

    /**
     * Get shipments by status.
     */
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByStatus(ShipmentStatus status) {
        log.debug("Fetching shipments with status: {}", status);
        return shipmentRepository.findByStatus(status);
    }

    /**
     * Get shipments by multiple statuses.
     */
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByStatuses(List<ShipmentStatus> statuses) {
        log.debug("Fetching shipments with statuses: {}", statuses);
        return shipmentRepository.findByStatusIn(statuses);
    }

    /**
     * Cancel a shipment.
     */
    @Transactional
    public Shipment cancelShipment(UUID shipmentId, String reason) {
        MDC.put("operation", "cancelShipment");
        MDC.put("shipmentId", shipmentId.toString());

        log.info("Cancelling shipment: {}, reason: {}", shipmentId, reason);

        Shipment shipment = getShipment(shipmentId);

        // Business logic validation in entity
        shipment.cancel();

        Shipment savedShipment = shipmentRepository.save(shipment);
        log.info("Shipment cancelled successfully: {}", shipmentId);

        // Add tracking event
        String location = trackingService.generateLocationForStatus(ShipmentStatus.CANCELLED);
        String description = "Shipment cancelled. Reason: " + reason;
        trackingService.addTrackingEvent(
                shipmentId,
                ShipmentStatus.CANCELLED,
                location,
                description
        );

        return savedShipment;
    }

    /**
     * Update shipment status (admin/simulation use).
     */
    @Transactional
    public Shipment updateShipmentStatus(UUID shipmentId, ShipmentStatus newStatus, String reason) {
        MDC.put("operation", "updateShipmentStatus");
        MDC.put("shipmentId", shipmentId.toString());

        log.info("Updating shipment status: shipmentId={}, newStatus={}, reason={}",
                shipmentId, newStatus, reason);

        Shipment shipment = getShipment(shipmentId);
        ShipmentStatus previousStatus = shipment.getStatus();

        // Use business methods for standard transitions
        switch (newStatus) {
            case CREATED -> shipment.markAsCreated();
            case IN_TRANSIT -> shipment.markAsInTransit();
            case OUT_FOR_DELIVERY -> shipment.markAsOutForDelivery();
            case DELIVERED -> shipment.markAsDelivered();
            case CANCELLED -> shipment.cancel();
            case RETURNED -> shipment.markAsReturned();
            default -> throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        Shipment savedShipment = shipmentRepository.save(shipment);
        log.info("Shipment status updated: {} -> {}", previousStatus, newStatus);

        // Add tracking event
        String location = trackingService.generateLocationForStatus(newStatus);
        String description = reason != null ? reason :
                trackingService.generateDescriptionForStatus(newStatus);
        trackingService.addTrackingEvent(
                shipmentId,
                newStatus,
                location,
                description
        );

        return savedShipment;
    }

    /**
     * Calculate shipping fee (flat rate for MVP).
     */
    public BigDecimal calculateShippingFee() {
        return shippingConfig.flatRateFee();
    }

    /**
     * Get all shipments.
     */
    @Transactional(readOnly = true)
    public List<Shipment> getAllShipments() {
        log.debug("Fetching all shipments");
        return shipmentRepository.findAll();
    }
}
