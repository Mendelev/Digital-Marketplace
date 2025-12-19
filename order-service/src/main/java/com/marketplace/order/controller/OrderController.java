package com.marketplace.order.controller;

import com.marketplace.order.domain.model.OrderStatus;
import com.marketplace.order.service.OrderService;
import com.marketplace.shared.dto.order.CreateOrderRequest;
import com.marketplace.shared.dto.order.OrderResponse;
import com.marketplace.shared.dto.order.OrderSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API controller for order operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order from cart.
     *
     * POST /api/v1/orders
     */
    @PostMapping
    @Operation(
        summary = "Create order",
        description = """
            Create a new order from a shopping cart. This operation:
            1. Fetches cart items from Cart Service
            2. Fetches shipping/billing addresses from User Service
            3. Creates order with frozen product/address snapshots
            4. Authorizes payment (90% success rate in mock)
            5. Reserves inventory with 15-minute TTL
            6. Publishes OrderCreated event to Kafka
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "402", description = "Payment required - payment authorization failed"),
        @ApiResponse(responseCode = "422", description = "Unprocessable entity - insufficient stock"),
        @ApiResponse(responseCode = "503", description = "Service unavailable - Cart or User service is down")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "Order creation request with user, cart, and address IDs", required = true)
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.userId());

        OrderResponse order = orderService.createOrder(request);

        log.info("Order created successfully: {}", order.orderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Get order by ID.
     *
     * GET /api/v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    @Operation(
        summary = "Get order",
        description = "Retrieve complete order details including items, addresses, and payment information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId) {
        log.debug("Fetching order: {}", orderId);

        OrderResponse order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * Get orders for a user.
     *
     * GET /api/v1/orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get user orders",
        description = "Retrieve paginated list of orders for a specific user with summary information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<Page<OrderSummaryResponse>> getUserOrders(
            @Parameter(description = "User ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID userId,
            @Parameter(description = "Pagination parameters (page, size, sort)")
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching orders for user: {}", userId);

        Page<OrderSummaryResponse> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Cancel an order.
     *
     * POST /api/v1/orders/{orderId}/cancel
     */
    @PostMapping("/{orderId}/cancel")
    @Operation(
        summary = "Cancel order",
        description = """
            Cancel an order if it hasn't been shipped yet.

            This operation:
            - Releases inventory reservation
            - Voids payment authorization
            - Updates order status to CANCELLED
            - Publishes OrderCancelled event

            Can only cancel orders in states: PENDING_PAYMENT, PAYMENT_AUTHORIZED, INVENTORY_RESERVED, CONFIRMED
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - order cannot be cancelled in current state"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId,
            @Parameter(description = "Cancellation reason", example = "Customer requested cancellation")
            @RequestParam(required = false, defaultValue = "Customer requested cancellation") String reason) {
        log.info("Cancelling order: {}, reason: {}", orderId, reason);

        OrderResponse order = orderService.cancelOrder(orderId, reason);

        log.info("Order cancelled successfully: {}", orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * Update order status (internal/admin use).
     *
     * PATCH /api/v1/orders/{orderId}/status
     */
    @PatchMapping("/{orderId}/status")
    @Operation(
        summary = "Update order status",
        description = """
            Update order status for admin operations and shipping updates.

            Valid transitions are enforced by the order state machine.
            Updates trigger OrderStatusChanged events.

            Common use cases:
            - Mark order as SHIPPED when shipped
            - Mark order as DELIVERED when delivered
            - Handle returns with REFUNDED status
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid status transition"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId,
            @Parameter(description = "New order status", required = true, example = "SHIPPED")
            @RequestParam OrderStatus status) {
        log.info("Updating order {} to status: {}", orderId, status);

        OrderResponse order = orderService.updateOrderStatus(orderId, status);

        log.info("Order status updated successfully: {}", orderId);
        return ResponseEntity.ok(order);
    }
}
