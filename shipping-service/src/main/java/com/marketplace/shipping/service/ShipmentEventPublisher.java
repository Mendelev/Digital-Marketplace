package com.marketplace.shipping.service;

import com.marketplace.shipping.domain.model.Shipment;
import com.marketplace.shipping.domain.model.ShipmentEvent;
import com.marketplace.shipping.domain.model.ShipmentStatus;
import com.marketplace.shipping.domain.repository.ShipmentEventRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for publishing shipment events to Kafka.
 */
@Service
public class ShipmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventPublisher.class);

    private final ShipmentEventRepository eventRepository;
    private final StreamBridge streamBridge;
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    public ShipmentEventPublisher(ShipmentEventRepository eventRepository,
                                 StreamBridge streamBridge) {
        this.eventRepository = eventRepository;
        this.streamBridge = streamBridge;
    }

    @PostConstruct
    public void initSequence() {
        Long maxSequence = eventRepository.findMaxSequenceNumber();
        sequenceGenerator.set(maxSequence != null ? maxSequence : 0);
        log.info("Initialized shipment event sequence number at: {}", sequenceGenerator.get());
    }

    /**
     * Publish ShipmentCreated event.
     */
    public void publishShipmentCreated(Shipment shipment) {
        publishEvent(
                "ShipmentCreated",
                shipment.getShipmentId(),
                Map.of(
                        "shipmentId", shipment.getShipmentId().toString(),
                        "orderId", shipment.getOrderId().toString(),
                        "userId", shipment.getUserId().toString(),
                        "status", shipment.getStatus().name(),
                        "trackingNumber", shipment.getTrackingNumber(),
                        "carrier", shipment.getCarrier(),
                        "shippingFee", shipment.getShippingFee().toString(),
                        "currency", shipment.getCurrency(),
                        "estimatedDeliveryDate", shipment.getEstimatedDeliveryDate() != null ?
                                shipment.getEstimatedDeliveryDate().toString() : "",
                        "createdAt", shipment.getCreatedAt().toString()
                )
        );
    }

    /**
     * Publish ShipmentUpdated event.
     */
    public void publishShipmentUpdated(Shipment shipment, ShipmentStatus previousStatus) {
        publishEvent(
                "ShipmentUpdated",
                shipment.getShipmentId(),
                Map.of(
                        "shipmentId", shipment.getShipmentId().toString(),
                        "orderId", shipment.getOrderId().toString(),
                        "userId", shipment.getUserId().toString(),
                        "previousStatus", previousStatus.name(),
                        "currentStatus", shipment.getStatus().name(),
                        "trackingNumber", shipment.getTrackingNumber(),
                        "updatedAt", shipment.getUpdatedAt().toString()
                )
        );
    }

    /**
     * Publish ShipmentDelivered event.
     */
    public void publishShipmentDelivered(Shipment shipment) {
        publishEvent(
                "ShipmentDelivered",
                shipment.getShipmentId(),
                Map.of(
                        "shipmentId", shipment.getShipmentId().toString(),
                        "orderId", shipment.getOrderId().toString(),
                        "userId", shipment.getUserId().toString(),
                        "trackingNumber", shipment.getTrackingNumber(),
                        "deliveredAt", shipment.getDeliveredAt() != null ?
                                shipment.getDeliveredAt().toString() : OffsetDateTime.now().toString(),
                        "actualDeliveryDate", shipment.getActualDeliveryDate() != null ?
                                shipment.getActualDeliveryDate().toString() : OffsetDateTime.now().toString()
                )
        );
    }

    /**
     * Publish ShipmentCancelled event.
     */
    public void publishShipmentCancelled(Shipment shipment, String reason) {
        publishEvent(
                "ShipmentCancelled",
                shipment.getShipmentId(),
                Map.of(
                        "shipmentId", shipment.getShipmentId().toString(),
                        "orderId", shipment.getOrderId().toString(),
                        "userId", shipment.getUserId().toString(),
                        "trackingNumber", shipment.getTrackingNumber(),
                        "reason", reason != null ? reason : "Shipment cancelled",
                        "cancelledAt", shipment.getUpdatedAt().toString()
                )
        );
    }

    /**
     * Publish event to Kafka with idempotency.
     */
    private void publishEvent(String eventType, UUID shipmentId, Map<String, Object> payload) {
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = sequenceGenerator.incrementAndGet();

        MDC.put("eventType", eventType);
        MDC.put("eventId", eventId.toString());
        MDC.put("shipmentId", shipmentId.toString());

        // Check idempotency
        if (eventRepository.findByEventId(eventId).isPresent()) {
            log.warn("Event {} already exists, skipping publication", eventId);
            return;
        }

        // Save event to database first (event sourcing)
        ShipmentEvent event = new ShipmentEvent(
                eventId,
                shipmentId,
                eventType,
                sequenceNumber,
                payload,
                OffsetDateTime.now()
        );

        eventRepository.save(event);
        log.info("Saved {} event (id: {}, sequence: {}) for shipment: {}",
                eventType, eventId, sequenceNumber, shipmentId);

        // Publish to Kafka with ordering key
        Message<ShipmentEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.KEY, shipmentId.toString())  // Ensures ordering per shipment
                .setHeader("event-id", eventId.toString())
                .setHeader("sequence-number", sequenceNumber)
                .setHeader("shipmentId", shipmentId.toString())
                .setHeader("correlationId", MDC.get("correlationId"))
                .build();

        try {
            boolean sent = streamBridge.send("shippingEvents-out-0", message);
            if (sent) {
                log.info("Published {} event to Kafka for shipment: {}", eventType, shipmentId);
            } else {
                log.error("Failed to publish {} event to Kafka for shipment: {}", eventType, shipmentId);
            }
        } catch (Exception e) {
            log.warn("Skipping Kafka publish for {} event (shipment: {}): {}", eventType, shipmentId, e.getMessage());
        }
    }
}
