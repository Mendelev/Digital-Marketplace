package com.marketplace.order.service;

import com.marketplace.order.client.CartServiceClient;
import com.marketplace.order.client.CartServiceClient.CartItemResponse;
import com.marketplace.order.client.CartServiceClient.CartResponse;
import com.marketplace.order.client.UserServiceClient;
import com.marketplace.order.client.UserServiceClient.AddressResponse;
import com.marketplace.order.config.OrderServiceProperties;
import com.marketplace.order.domain.model.*;
import com.marketplace.order.domain.repository.OrderRepository;
import com.marketplace.order.dto.AddressSnapshot;
import com.marketplace.order.dto.PaymentResponse;
import com.marketplace.order.dto.ReservationResponse;
import com.marketplace.order.exception.OrderCancellationException;
import com.marketplace.order.exception.PaymentFailedException;
import com.marketplace.order.exception.ResourceNotFoundException;
import com.marketplace.shared.dto.order.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main orchestration service for order operations.
 * Coordinates payment, inventory, and event publishing.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final UserServiceClient userServiceClient;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final OrderStateMachine stateMachine;
    private final OrderEventPublisher eventPublisher;
    private final OrderServiceProperties properties;

    public OrderService(OrderRepository orderRepository,
                        CartServiceClient cartServiceClient,
                        UserServiceClient userServiceClient,
                        PaymentService paymentService,
                        InventoryService inventoryService,
                        OrderStateMachine stateMachine,
                        OrderEventPublisher eventPublisher,
                        OrderServiceProperties properties) {
        this.orderRepository = orderRepository;
        this.cartServiceClient = cartServiceClient;
        this.userServiceClient = userServiceClient;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    /**
     * Create a new order from cart.
     * Orchestrates the entire order creation flow:
     * 1. Fetch cart and addresses
     * 2. Create order with snapshots
     * 3. Authorize payment
     * 4. Reserve inventory
     * 5. Confirm order
     * 6. Publish events
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}, cart: {}", request.userId(), request.cartId());

        try {
            // 1. Fetch cart from Cart Service
            CartResponse cart = cartServiceClient.getCart(request.cartId());
            log.debug("Fetched cart with {} items, subtotal: {}", cart.items().size(), cart.subtotalAmount());

            // 2. Fetch addresses from User Service
            AddressResponse shippingAddr = userServiceClient.getAddress(request.shippingAddressId());
            AddressResponse billingAddr = userServiceClient.getAddress(request.billingAddressId());
            log.debug("Fetched shipping and billing addresses");

            // 3. Create order entity
            Order order = new Order();
            order.setUserId(request.userId());
            order.setCartId(request.cartId());
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setCurrency("USD");

            // Set address snapshots
            order.setShippingAddressSnapshot(toAddressSnapshot(shippingAddr));
            order.setBillingAddressSnapshot(toAddressSnapshot(billingAddr));

            // Add order items from cart
            for (CartItemResponse cartItem : cart.items()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(cartItem.productId());
                orderItem.setSku(cartItem.sku());
                orderItem.setTitleSnapshot(cartItem.titleSnapshot());
                orderItem.setUnitPriceSnapshot(cartItem.unitPriceSnapshot());
                orderItem.setQuantity(cartItem.quantity());
                orderItem.setLineTotalAmount(cartItem.lineTotalAmount());
                order.addItem(orderItem);
            }

            // Calculate amounts
            BigDecimal subtotal = cart.subtotalAmount();
            BigDecimal shipping = properties.flatShippingRate();
            BigDecimal tax = BigDecimal.ZERO; // TODO: Calculate tax
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal total = subtotal.add(shipping).add(tax).subtract(discount);

            order.setSubtotalAmount(subtotal);
            order.setShippingAmount(shipping);
            order.setTaxAmount(tax);
            order.setDiscountAmount(discount);
            order.setTotalAmount(total);

            Order savedOrder = orderRepository.save(order);
            log.info("Order created: {}", savedOrder.getOrderId());

            // Publish OrderCreated event
            eventPublisher.publishOrderCreated(savedOrder);

            // 4. Authorize payment
            try {
                PaymentResponse payment = paymentService.authorizePayment(
                        savedOrder.getOrderId(),
                        request.userId(),
                        total,
                        "USD"
                );

                savedOrder.setPaymentId(payment.paymentId());
                stateMachine.transitionTo(savedOrder, OrderStatus.PAYMENT_AUTHORIZED);
                orderRepository.save(savedOrder);

                log.info("Payment authorized for order: {}", savedOrder.getOrderId());
                eventPublisher.publishOrderStatusChanged(savedOrder, OrderStatus.PENDING_PAYMENT.name());

            } catch (PaymentFailedException e) {
                // Payment failed - update order status and publish event
                stateMachine.transitionTo(savedOrder, OrderStatus.PAYMENT_FAILED);
                orderRepository.save(savedOrder);
                eventPublisher.publishOrderPaymentFailed(savedOrder, e.getMessage());

                log.error("Payment failed for order: {}", savedOrder.getOrderId(), e);
                throw e;
            }

            // 5. Reserve inventory
            ReservationResponse reservation = inventoryService.reserveStock(
                    savedOrder.getOrderId(),
                    savedOrder.getItems()
            );

            stateMachine.transitionTo(savedOrder, OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(savedOrder);
            log.info("Inventory reserved for order: {}, reservation: {}",
                    savedOrder.getOrderId(), reservation.reservationId());

            // 6. Confirm order
            stateMachine.transitionTo(savedOrder, OrderStatus.CONFIRMED);
            orderRepository.save(savedOrder);

            log.info("Order confirmed: {}", savedOrder.getOrderId());
            eventPublisher.publishOrderConfirmed(savedOrder);

            return toOrderResponse(savedOrder);

        } catch (PaymentFailedException e) {
            // Payment failed - order already updated, just rethrow
            throw e;
        } catch (Exception e) {
            log.error("Error creating order for user: {}", request.userId(), e);
            throw e;
        }
    }

    /**
     * Get order by ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        log.debug("Fetching order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return toOrderResponse(order);
    }

    /**
     * Get orders for a user.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getUserOrders(UUID userId, Pageable pageable) {
        log.debug("Fetching orders for user: {}", userId);

        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(this::toOrderSummaryResponse);
    }

    /**
     * Cancel an order.
     * Only orders that haven't been shipped can be cancelled.
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String reason) {
        log.info("Cancelling order: {}, reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Check if order can be cancelled
        if (!order.canBeCancelled()) {
            throw new OrderCancellationException(
                    "Order cannot be cancelled in status: " + order.getStatus());
        }

        String previousStatus = order.getStatus().name();

        // Release inventory reservation
        inventoryService.releaseReservation(orderId);

        // Void payment if authorized
        if (order.getPaymentId() != null &&
                (order.getStatus() == OrderStatus.PAYMENT_AUTHORIZED ||
                        order.getStatus() == OrderStatus.INVENTORY_RESERVED ||
                        order.getStatus() == OrderStatus.CONFIRMED)) {
            try {
                paymentService.voidPayment(order.getPaymentId());
                log.info("Payment voided for order: {}", orderId);
            } catch (Exception e) {
                log.error("Failed to void payment for order: {}", orderId, e);
            }
        }

        // Update order status
        stateMachine.transitionTo(order, OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order cancelled: {}", orderId);
        eventPublisher.publishOrderCancelled(order, reason);

        return toOrderResponse(order);
    }

    /**
     * Update order status (for shipping updates, etc.).
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        log.info("Updating order {} to status: {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        String previousStatus = order.getStatus().name();

        // Use state machine to validate transition
        stateMachine.transitionTo(order, newStatus);
        orderRepository.save(order);

        log.info("Order status updated: {} -> {}", previousStatus, newStatus);
        eventPublisher.publishOrderStatusChanged(order, previousStatus);

        // Confirm reservation if order is shipped
        if (newStatus == OrderStatus.SHIPPED) {
            try {
                inventoryService.confirmReservation(orderId);
                log.info("Reservation confirmed for shipped order: {}", orderId);
            } catch (Exception e) {
                log.error("Failed to confirm reservation for order: {}", orderId, e);
            }
        }

        return toOrderResponse(order);
    }

    /**
     * Convert Address response to snapshot.
     */
    private AddressSnapshot toAddressSnapshot(AddressResponse address) {
        return new AddressSnapshot(
                address.label(),
                address.country(),
                address.state(),
                address.city(),
                address.zip(),
                address.street(),
                address.number(),
                address.complement()
        );
    }

    /**
     * Convert AddressSnapshot to response DTO.
     */
    private AddressSnapshotResponse toAddressSnapshotResponse(AddressSnapshot snapshot) {
        return new AddressSnapshotResponse(
                snapshot.label(),
                snapshot.country(),
                snapshot.state(),
                snapshot.city(),
                snapshot.zip(),
                snapshot.street(),
                snapshot.number(),
                snapshot.complement()
        );
    }

    /**
     * Convert Order entity to response DTO.
     */
    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getOrderItemId(),
                        item.getProductId(),
                        item.getSku(),
                        item.getTitleSnapshot(),
                        item.getUnitPriceSnapshot(),
                        item.getQuantity(),
                        item.getLineTotalAmount()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getCurrency(),
                order.getSubtotalAmount(),
                order.getShippingAmount(),
                order.getTaxAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getPaymentId(),
                order.getCartId(),
                toAddressSnapshotResponse(order.getShippingAddressSnapshot()),
                toAddressSnapshotResponse(order.getBillingAddressSnapshot()),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    /**
     * Convert Order entity to summary response DTO.
     */
    private OrderSummaryResponse toOrderSummaryResponse(Order order) {
        return new OrderSummaryResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getItems().size(),
                order.getCreatedAt()
        );
    }
}
