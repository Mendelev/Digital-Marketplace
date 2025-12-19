package com.marketplace.order.service;

import com.marketplace.order.domain.model.Order;
import com.marketplace.order.domain.model.OrderEvent;
import com.marketplace.order.domain.repository.OrderEventRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Service for publishing order events to Kafka.
 * Implements event sourcing pattern with idempotency and ordering guarantees.
 */
@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String BINDING_NAME = "order-events-out-0";

    private final OrderEventRepository eventRepository;
    private final StreamBridge streamBridge;
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    public OrderEventPublisher(OrderEventRepository eventRepository, StreamBridge streamBridge) {
        this.eventRepository = eventRepository;
        this.streamBridge = streamBridge;
    }

    @PostConstruct
    public void initSequence() {
        Long maxSequence = eventRepository.findMaxSequenceNumber();
        sequenceGenerator.set(maxSequence != null ? maxSequence : 0);
        log.info("Initialized order event sequence number at: {}", sequenceGenerator.get());
    }

    /**
     * Publish OrderCreated event.
     */
    public void publishOrderCreated(Order order) {
        publishEvent(
                "OrderCreated",
                order.getOrderId(),
                Map.of(
                        "orderId", order.getOrderId().toString(),
                        "userId", order.getUserId().toString(),
                        "status", order.getStatus().name(),
                        "totalAmount", order.getTotalAmount().toString(),
                        "currency", order.getCurrency(),
                        "cartId", order.getCartId().toString(),
                        "itemCount", order.getItems().size(),
                        "createdAt", order.getCreatedAt().toString()
                )
        );
    }

    /**
     * Publish OrderConfirmed event.
     */
    public void publishOrderConfirmed(Order order) {
        publishEvent(
                "OrderConfirmed",
                order.getOrderId(),
                Map.of(
                        "orderId", order.getOrderId().toString(),
                        "userId", order.getUserId().toString(),
                        "status", order.getStatus().name(),
                        "paymentId", order.getPaymentId().toString(),
                        "totalAmount", order.getTotalAmount().toString(),
                        "confirmedAt", OffsetDateTime.now().toString()
                )
        );
    }

    /**
     * Publish OrderCancelled event.
     */
    public void publishOrderCancelled(Order order, String reason) {
        publishEvent(
                "OrderCancelled",
                order.getOrderId(),
                Map.of(
                        "orderId", order.getOrderId().toString(),
                        "userId", order.getUserId().toString(),
                        "status", order.getStatus().name(),
                        "reason", reason,
                        "cancelledAt", OffsetDateTime.now().toString()
                )
        );
    }

    /**
     * Publish OrderStatusChanged event.
     */
    public void publishOrderStatusChanged(Order order, String previousStatus) {
        publishEvent(
                "OrderStatusChanged",
                order.getOrderId(),
                Map.of(
                        "orderId", order.getOrderId().toString(),
                        "userId", order.getUserId().toString(),
                        "previousStatus", previousStatus,
                        "newStatus", order.getStatus().name(),
                        "changedAt", OffsetDateTime.now().toString()
                )
        );
    }

    /**
     * Publish OrderPaymentFailed event.
     */
    public void publishOrderPaymentFailed(Order order, String reason) {
        publishEvent(
                "OrderPaymentFailed",
                order.getOrderId(),
                Map.of(
                        "orderId", order.getOrderId().toString(),
                        "userId", order.getUserId().toString(),
                        "status", order.getStatus().name(),
                        "reason", reason,
                        "failedAt", OffsetDateTime.now().toString()
                )
        );
    }

    /**
     * Core event publishing logic with idempotency and ordering.
     */
    private void publishEvent(String eventType, UUID orderId, Map<String, Object> payload) {
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = sequenceGenerator.incrementAndGet();

        // Check idempotency
        if (eventRepository.findByEventId(eventId).isPresent()) {
            log.warn("Event {} already exists, skipping publication", eventId);
            return;
        }

        // Save event to database first (event sourcing)
        OrderEvent event = new OrderEvent(
                eventId,
                orderId,
                eventType,
                sequenceNumber,
                payload,
                OffsetDateTime.now()
        );

        eventRepository.save(event);
        log.info("Saved {} event (id: {}, sequence: {}) for order: {}",
                eventType, eventId, sequenceNumber, orderId);

        // Publish to Kafka with ordering key (orderId ensures ordering per order)
        Message<OrderEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.KEY, orderId.toString())  // Ensures ordering per order
                .setHeader("event-id", eventId.toString())
                .setHeader("sequence-number", sequenceNumber)
                .setHeader("orderId", orderId.toString())
                .build();

        boolean sent = streamBridge.send(BINDING_NAME, message);
        if (sent) {
            log.info("Published {} event to Kafka for order: {}", eventType, orderId);
        } else {
            log.error("Failed to publish {} event to Kafka for order: {}", eventType, orderId);
        }
    }
}
