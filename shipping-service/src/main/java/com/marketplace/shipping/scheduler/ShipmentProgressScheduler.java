package com.marketplace.shipping.scheduler;

import com.marketplace.shipping.domain.model.Shipment;
import com.marketplace.shipping.domain.model.ShipmentStatus;
import com.marketplace.shipping.service.ShipmentEventPublisher;
import com.marketplace.shipping.service.ShipmentService;
import com.marketplace.shipping.service.ShipmentSimulationService;
import com.marketplace.shipping.service.ShipmentTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for automatically progressing shipments through statuses.
 */
@Component
public class ShipmentProgressScheduler {

    private static final Logger log = LoggerFactory.getLogger(ShipmentProgressScheduler.class);

    private final ShipmentService shipmentService;
    private final ShipmentSimulationService simulationService;
    private final ShipmentTrackingService trackingService;
    private final ShipmentEventPublisher eventPublisher;

    public ShipmentProgressScheduler(ShipmentService shipmentService,
                                    ShipmentSimulationService simulationService,
                                    ShipmentTrackingService trackingService,
                                    ShipmentEventPublisher eventPublisher) {
        this.shipmentService = shipmentService;
        this.simulationService = simulationService;
        this.trackingService = trackingService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Periodically progress eligible shipments.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void progressShipments() {
        try {
            log.debug("Starting shipment progression check");

            // Get shipments that can progress
            List<ShipmentStatus> progressableStatuses = List.of(
                    ShipmentStatus.CREATED,
                    ShipmentStatus.IN_TRANSIT,
                    ShipmentStatus.OUT_FOR_DELIVERY
            );

            List<Shipment> shipments = shipmentService.getShipmentsByStatuses(progressableStatuses);

            if (shipments.isEmpty()) {
                log.debug("No shipments eligible for progression");
                return;
            }

            log.info("Found {} shipments to check for progression", shipments.size());

            int progressedCount = 0;
            for (Shipment shipment : shipments) {
                try {
                    if (processShipment(shipment)) {
                        progressedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error progressing shipment {}: {}",
                            shipment.getShipmentId(), e.getMessage(), e);
                    // Continue processing other shipments
                }
            }

            if (progressedCount > 0) {
                log.info("Successfully progressed {} shipments", progressedCount);
            }

        } catch (Exception e) {
            log.error("Error in shipment progression scheduler", e);
        }
    }

    /**
     * Process a single shipment for progression.
     * Returns true if shipment was progressed.
     */
    private boolean processShipment(Shipment shipment) {
        ShipmentStatus currentStatus = shipment.getStatus();

        // Check if shipment should progress
        if (!simulationService.shouldProgress(shipment)) {
            return false;
        }

        // Get next status
        ShipmentStatus nextStatus = simulationService.getNextStatus(currentStatus);
        if (nextStatus == null) {
            log.debug("No next status for shipment {} in status {}",
                    shipment.getShipmentId(), currentStatus);
            return false;
        }

        log.info("Progressing shipment {} from {} to {}",
                shipment.getShipmentId(), currentStatus, nextStatus);

        // Update shipment status
        String reason = "Automatic status progression via simulation";
        Shipment updatedShipment = shipmentService.updateShipmentStatus(
                shipment.getShipmentId(),
                nextStatus,
                reason
        );

        // Publish appropriate event
        if (nextStatus == ShipmentStatus.DELIVERED) {
            eventPublisher.publishShipmentDelivered(updatedShipment);
        } else {
            eventPublisher.publishShipmentUpdated(updatedShipment, currentStatus);
        }

        return true;
    }
}
