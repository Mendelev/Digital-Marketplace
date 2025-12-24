package com.marketplace.shipping.consumer;

import com.marketplace.shipping.domain.model.AddressSnapshot;
import com.marketplace.shipping.domain.model.Shipment;
import com.marketplace.shipping.dto.CreateShipmentRequest;
import com.marketplace.shipping.service.ShipmentEventPublisher;
import com.marketplace.shipping.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Consumer for order events from Kafka.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ShipmentService shipmentService;
    private final ShipmentEventPublisher eventPublisher;

    public OrderEventConsumer(ShipmentService shipmentService,
                             ShipmentEventPublisher eventPublisher) {
        this.shipmentService = shipmentService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Functional bean for consuming order events.
     */
    @Bean
    public Consumer<Message<Map<String, Object>>> orderEvents() {
        return message -> {
            try {
                Map<String, Object> event = message.getPayload();
                String eventType = (String) event.get("eventType");

                // Set correlation ID if present in headers
                Object correlationId = message.getHeaders().get("correlationId");
                if (correlationId != null) {
                    MDC.put("correlationId", correlationId.toString());
                }

                log.info("Received order event: type={}, eventId={}",
                        eventType, event.get("eventId"));

                if ("OrderConfirmed".equals(eventType)) {
                    handleOrderConfirmed(event);
                } else {
                    log.debug("Ignoring event type: {}", eventType);
                }

            } catch (Exception e) {
                log.error("Error processing order event", e);
                // Don't throw - let Kafka retry based on configuration
            } finally {
                MDC.clear();
            }
        };
    }

    /**
     * Handle OrderConfirmed event by creating a shipment.
     */
    @SuppressWarnings("unchecked")
    private void handleOrderConfirmed(Map<String, Object> event) {
        try {
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");

            UUID orderId = UUID.fromString((String) payload.get("orderId"));
            UUID userId = UUID.fromString((String) payload.get("userId"));

            MDC.put("orderId", orderId.toString());
            MDC.put("userId", userId.toString());

            log.info("Processing OrderConfirmed event for order: {}", orderId);

            // Check if shipment already exists (idempotency)
            try {
                Shipment existingShipment = shipmentService.getShipmentByOrderId(orderId);
                log.info("Shipment already exists for order {}: {}, skipping creation",
                        orderId, existingShipment.getShipmentId());
                return;
            } catch (Exception e) {
                // Shipment doesn't exist, proceed with creation
            }

            // Extract shipping address from payload
            Map<String, Object> addressData = (Map<String, Object>) payload.get("shippingAddress");
            if (addressData == null) {
                log.error("No shipping address in OrderConfirmed event for order: {}", orderId);
                return;
            }

            AddressSnapshot shippingAddress = new AddressSnapshot(
                    (String) addressData.get("label"),
                    (String) addressData.get("country"),
                    (String) addressData.get("state"),
                    (String) addressData.get("city"),
                    (String) addressData.get("zip"),
                    (String) addressData.get("street"),
                    (String) addressData.get("number"),
                    (String) addressData.get("complement")
            );

            // Extract item count (default to 1 if not present)
            Integer itemCount = payload.containsKey("itemCount") ?
                    ((Number) payload.get("itemCount")).intValue() : 1;

            // Create shipment request
            CreateShipmentRequest request = new CreateShipmentRequest(
                    orderId,
                    userId,
                    shippingAddress,
                    itemCount,
                    null,  // packageWeightKg
                    null   // packageDimensions
            );

            // Create shipment
            Shipment shipment = shipmentService.createShipment(request);
            log.info("Shipment created from OrderConfirmed event: shipmentId={}, trackingNumber={}",
                    shipment.getShipmentId(), shipment.getTrackingNumber());

            // Publish ShipmentCreated event
            eventPublisher.publishShipmentCreated(shipment);

        } catch (Exception e) {
            log.error("Failed to process OrderConfirmed event", e);
            throw e; // Re-throw for Kafka retry
        }
    }
}
