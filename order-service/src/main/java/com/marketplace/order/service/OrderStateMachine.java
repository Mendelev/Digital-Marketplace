package com.marketplace.order.service;

import com.marketplace.order.domain.model.Order;
import com.marketplace.order.domain.model.OrderStatus;
import com.marketplace.order.exception.InvalidOrderStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * State machine service for managing order status transitions.
 * Enforces valid state transitions and prevents invalid order state changes.
 */
@Service
public class OrderStateMachine {

    private static final Logger log = LoggerFactory.getLogger(OrderStateMachine.class);

    /**
     * Allowed state transitions map.
     * Key: Current status
     * Value: Set of allowed next statuses
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAYMENT, Set.of(
                    OrderStatus.PAYMENT_AUTHORIZED,
                    OrderStatus.PAYMENT_FAILED,
                    OrderStatus.CANCELLED
            ),
            OrderStatus.PAYMENT_AUTHORIZED, Set.of(
                    OrderStatus.INVENTORY_RESERVED,
                    OrderStatus.CANCELLED
            ),
            OrderStatus.PAYMENT_FAILED, Set.of(
                    OrderStatus.CANCELLED
            ),
            OrderStatus.INVENTORY_RESERVED, Set.of(
                    OrderStatus.CONFIRMED,
                    OrderStatus.CANCELLED
            ),
            OrderStatus.CONFIRMED, Set.of(
                    OrderStatus.SHIPPED,
                    OrderStatus.CANCELLED
            ),
            OrderStatus.SHIPPED, Set.of(
                    OrderStatus.DELIVERED,
                    OrderStatus.REFUNDED
            ),
            OrderStatus.DELIVERED, Set.of(
                    OrderStatus.REFUNDED
            ),
            OrderStatus.CANCELLED, Set.of(
                    // Terminal state - no transitions allowed
            ),
            OrderStatus.REFUNDED, Set.of(
                    // Terminal state - no transitions allowed
            )
    );

    /**
     * Transition order to a new status.
     * Validates that the transition is allowed before making the change.
     *
     * @param order     Order to transition
     * @param newStatus Target status
     * @throws InvalidOrderStateException if transition is not allowed
     */
    public void transitionTo(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();

        log.debug("Attempting state transition: {} -> {} for order: {}",
                currentStatus, newStatus, order.getOrderId());

        // Check if transition is allowed
        Set<OrderStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.get(currentStatus);

        if (allowedNextStatuses == null || !allowedNextStatuses.contains(newStatus)) {
            String message = String.format(
                    "Invalid state transition for order %s: %s -> %s. Allowed transitions from %s: %s",
                    order.getOrderId(), currentStatus, newStatus, currentStatus,
                    allowedNextStatuses != null ? allowedNextStatuses : "none"
            );
            log.error(message);
            throw new InvalidOrderStateException(message);
        }

        // Perform transition
        order.transitionTo(newStatus);

        log.info("State transition successful: {} -> {} for order: {}",
                currentStatus, newStatus, order.getOrderId());
    }

    /**
     * Check if a transition is valid without performing it.
     *
     * @param currentStatus Current order status
     * @param newStatus     Target status
     * @return true if transition is allowed, false otherwise
     */
    public boolean isTransitionAllowed(OrderStatus currentStatus, OrderStatus newStatus) {
        Set<OrderStatus> allowedNextStatuses = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowedNextStatuses != null && allowedNextStatuses.contains(newStatus);
    }

    /**
     * Get all allowed next statuses for a given current status.
     *
     * @param currentStatus Current order status
     * @return Set of allowed next statuses
     */
    public Set<OrderStatus> getAllowedTransitions(OrderStatus currentStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
    }
}
