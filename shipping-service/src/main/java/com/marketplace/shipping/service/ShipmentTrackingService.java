package com.marketplace.shipping.service;

import com.marketplace.shipping.domain.model.ShipmentStatus;
import com.marketplace.shipping.domain.model.ShipmentTracking;
import com.marketplace.shipping.domain.repository.ShipmentTrackingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing shipment tracking events.
 */
@Service
public class ShipmentTrackingService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentTrackingService.class);

    private final ShipmentTrackingRepository trackingRepository;

    public ShipmentTrackingService(ShipmentTrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;
    }

    /**
     * Add a tracking event for a shipment.
     */
    @Transactional
    public ShipmentTracking addTrackingEvent(UUID shipmentId, ShipmentStatus status,
                                            String location, String description) {
        MDC.put("shipmentId", shipmentId.toString());
        MDC.put("operation", "addTrackingEvent");

        log.info("Adding tracking event for shipment {}: status={}, location={}",
                shipmentId, status, location);

        ShipmentTracking tracking = new ShipmentTracking(
                shipmentId,
                status,
                location,
                description,
                OffsetDateTime.now()
        );

        ShipmentTracking saved = trackingRepository.save(tracking);
        log.info("Tracking event added successfully: id={}", saved.getId());

        return saved;
    }

    /**
     * Get tracking history for a shipment (most recent first).
     */
    @Transactional(readOnly = true)
    public List<ShipmentTracking> getTrackingHistory(UUID shipmentId) {
        MDC.put("shipmentId", shipmentId.toString());
        log.debug("Fetching tracking history for shipment: {}", shipmentId);

        return trackingRepository.findByShipmentIdOrderByEventTimeDesc(shipmentId);
    }

    /**
     * Get current location (latest tracking event).
     */
    @Transactional(readOnly = true)
    public Optional<ShipmentTracking> getCurrentLocation(UUID shipmentId) {
        MDC.put("shipmentId", shipmentId.toString());
        log.debug("Fetching current location for shipment: {}", shipmentId);

        return trackingRepository.findFirstByShipmentIdOrderByEventTimeDesc(shipmentId);
    }

    /**
     * Generate realistic location based on status.
     */
    public String generateLocationForStatus(ShipmentStatus status) {
        return switch (status) {
            case PENDING -> "Warehouse - Preparing for shipment";
            case CREATED -> "Distribution Center - Label created";
            case IN_TRANSIT -> "In Transit - En route to destination";
            case OUT_FOR_DELIVERY -> "Local Facility - Out for delivery";
            case DELIVERED -> "Delivered to recipient";
            case CANCELLED -> "Shipment cancelled";
            case RETURNED -> "Returned to sender";
        };
    }

    /**
     * Generate realistic description based on status.
     */
    public String generateDescriptionForStatus(ShipmentStatus status) {
        return switch (status) {
            case PENDING -> "Order received and awaiting processing";
            case CREATED -> "Shipping label generated, package ready for pickup";
            case IN_TRANSIT -> "Package picked up by carrier and in transit";
            case OUT_FOR_DELIVERY -> "Package is out for delivery to your address";
            case DELIVERED -> "Package successfully delivered";
            case CANCELLED -> "Shipment has been cancelled";
            case RETURNED -> "Package returned to sender";
        };
    }
}
