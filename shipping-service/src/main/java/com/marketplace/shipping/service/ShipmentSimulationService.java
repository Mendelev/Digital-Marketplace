package com.marketplace.shipping.service;

import com.marketplace.shipping.config.ShipmentSimulationConfig;
import com.marketplace.shipping.domain.model.Shipment;
import com.marketplace.shipping.domain.model.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Random;

/**
 * Service for simulating shipment progression.
 */
@Service
public class ShipmentSimulationService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentSimulationService.class);

    private final ShipmentSimulationConfig simulationConfig;
    private final Random random = new Random();

    public ShipmentSimulationService(ShipmentSimulationConfig simulationConfig) {
        this.simulationConfig = simulationConfig;
    }

    /**
     * Check if a shipment should progress to the next status.
     */
    public boolean shouldProgress(Shipment shipment) {
        if (!simulationConfig.autoProgressEnabled()) {
            return false;
        }

        ShipmentStatus currentStatus = shipment.getStatus();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime referenceTime = getReferenceTimeForStatus(shipment, currentStatus);

        if (referenceTime == null) {
            return false;
        }

        int requiredDelaySeconds = getDelayForStatus(currentStatus);
        Duration elapsed = Duration.between(referenceTime, now);

        boolean shouldProgress = elapsed.getSeconds() >= requiredDelaySeconds;

        if (shouldProgress) {
            log.debug("Shipment {} eligible for progression: status={}, elapsed={}s, required={}s",
                    shipment.getShipmentId(), currentStatus, elapsed.getSeconds(), requiredDelaySeconds);
        }

        return shouldProgress;
    }

    /**
     * Determine the next status for a shipment.
     */
    public ShipmentStatus getNextStatus(ShipmentStatus currentStatus) {
        return switch (currentStatus) {
            case CREATED -> ShipmentStatus.IN_TRANSIT;
            case IN_TRANSIT -> ShipmentStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> simulateDeliverySuccess() ?
                    ShipmentStatus.DELIVERED : ShipmentStatus.DELIVERED; // Could add RETURNED logic
            default -> null; // No progression for PENDING, DELIVERED, CANCELLED, RETURNED
        };
    }

    /**
     * Check if shipment can progress (has a valid next status).
     */
    public boolean canProgress(ShipmentStatus currentStatus) {
        return currentStatus == ShipmentStatus.CREATED ||
               currentStatus == ShipmentStatus.IN_TRANSIT ||
               currentStatus == ShipmentStatus.OUT_FOR_DELIVERY;
    }

    /**
     * Simulate delivery success based on configured success rate.
     */
    private boolean simulateDeliverySuccess() {
        return random.nextDouble() < simulationConfig.deliverySuccessRate();
    }

    /**
     * Get the reference time for calculating elapsed duration.
     */
    private OffsetDateTime getReferenceTimeForStatus(Shipment shipment, ShipmentStatus status) {
        return switch (status) {
            case CREATED -> shipment.getCreatedAt();
            case IN_TRANSIT -> shipment.getShippedAt();
            case OUT_FOR_DELIVERY -> shipment.getUpdatedAt(); // When it transitioned to OUT_FOR_DELIVERY
            default -> null;
        };
    }

    /**
     * Get required delay in seconds for a status.
     */
    private int getDelayForStatus(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> simulationConfig.createdToInTransitDelaySeconds();
            case IN_TRANSIT -> simulationConfig.inTransitToOutForDeliveryDelaySeconds();
            case OUT_FOR_DELIVERY -> simulationConfig.outForDeliveryToDeliveredDelaySeconds();
            default -> Integer.MAX_VALUE; // Never progress
        };
    }

    /**
     * Generate realistic tracking location for simulation.
     */
    public String generateSimulatedLocation(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> "Distribution Center - San Francisco, CA";
            case IN_TRANSIT -> getRandomTransitLocation();
            case OUT_FOR_DELIVERY -> "Local Delivery Hub - Customer Area";
            case DELIVERED -> "Customer Address";
            default -> "Processing Facility";
        };
    }

    /**
     * Get random transit location for realism.
     */
    private String getRandomTransitLocation() {
        String[] locations = {
                "Regional Hub - Los Angeles, CA",
                "Transit Center - Phoenix, AZ",
                "Sorting Facility - Denver, CO",
                "Distribution Center - Chicago, IL",
                "Transit Hub - Dallas, TX"
        };
        return locations[random.nextInt(locations.length)];
    }
}
