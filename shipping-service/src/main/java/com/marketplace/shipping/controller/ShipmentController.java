package com.marketplace.shipping.controller;

import com.marketplace.shipping.config.ServiceProperties;
import com.marketplace.shipping.domain.model.Shipment;
import com.marketplace.shipping.domain.model.ShipmentStatus;
import com.marketplace.shipping.domain.model.ShipmentTracking;
import com.marketplace.shipping.dto.CreateShipmentRequest;
import com.marketplace.shipping.dto.ShipmentResponse;
import com.marketplace.shipping.dto.ShipmentTrackingResponse;
import com.marketplace.shipping.dto.UpdateShipmentStatusRequest;
import com.marketplace.shipping.exception.InvalidTokenException;
import com.marketplace.shipping.filter.JwtValidationFilter;
import com.marketplace.shipping.security.AuthenticatedUser;
import com.marketplace.shipping.service.ShipmentEventPublisher;
import com.marketplace.shipping.service.ShipmentService;
import com.marketplace.shipping.service.ShipmentTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for shipment management.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@Tag(name = "Shipment Management", description = "APIs for managing shipments and tracking")
public class ShipmentController {

    private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);
    private static final String SERVICE_SECRET_HEADER = "X-Service-Secret";

    private final ShipmentService shipmentService;
    private final ShipmentTrackingService trackingService;
    private final ShipmentEventPublisher eventPublisher;
    private final ServiceProperties serviceProperties;

    public ShipmentController(ShipmentService shipmentService,
                             ShipmentTrackingService trackingService,
                             ShipmentEventPublisher eventPublisher,
                             ServiceProperties serviceProperties) {
        this.shipmentService = shipmentService;
        this.trackingService = trackingService;
        this.eventPublisher = eventPublisher;
        this.serviceProperties = serviceProperties;
    }

    // ========== Internal Endpoints (Service-to-Service) ==========

    @PostMapping
    @Operation(summary = "Create shipment", description = "Create a new shipment (service-to-service)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Shipment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Invalid service secret"),
            @ApiResponse(responseCode = "409", description = "Shipment already exists for order")
    })
    public ResponseEntity<ShipmentResponse> createShipment(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @Valid @RequestBody CreateShipmentRequest request) {

        validateServiceSecret(sharedSecret);

        log.info("Creating shipment for order: {}", request.orderId());

        Shipment shipment = shipmentService.createShipment(request);
        eventPublisher.publishShipmentCreated(shipment);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(shipment));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel shipment", description = "Cancel a shipment (service-to-service)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel shipment in current status"),
            @ApiResponse(responseCode = "403", description = "Invalid service secret"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentResponse> cancelShipment(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @PathVariable UUID id,
            @RequestBody(required = false) UpdateShipmentStatusRequest request) {

        validateServiceSecret(sharedSecret);

        String reason = request != null && request.reason() != null ?
                request.reason() : "Shipment cancelled by service";

        log.info("Cancelling shipment: {}", id);

        Shipment shipment = shipmentService.cancelShipment(id, reason);
        eventPublisher.publishShipmentCancelled(shipment, reason);

        return ResponseEntity.ok(mapToResponse(shipment));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get shipment by order ID", description = "Get shipment for an order (service-to-service)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment found"),
            @ApiResponse(responseCode = "403", description = "Invalid service secret"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentResponse> getShipmentByOrderId(
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret,
            @PathVariable UUID orderId) {

        validateServiceSecret(sharedSecret);

        log.debug("Fetching shipment for order: {}", orderId);

        Shipment shipment = shipmentService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(mapToResponse(shipment));
    }

    // ========== User Endpoints (JWT Auth) ==========

    @GetMapping("/{id}")
    @Operation(summary = "Get shipment by ID", description = "Get shipment details (requires JWT)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentResponse> getShipment(
            @PathVariable UUID id,
            HttpServletRequest request) {

        AuthenticatedUser user = getAuthenticatedUser(request);

        log.debug("User {} fetching shipment: {}", user.getUserId(), id);

        Shipment shipment = shipmentService.getShipment(id);
        verifyOwnership(user, shipment.getUserId());

        return ResponseEntity.ok(mapToResponse(shipment));
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "Get shipment tracking", description = "Get tracking history for shipment (requires JWT)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tracking history found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<List<ShipmentTrackingResponse>> getShipmentTracking(
            @PathVariable UUID id,
            HttpServletRequest request) {

        AuthenticatedUser user = getAuthenticatedUser(request);

        log.debug("User {} fetching tracking for shipment: {}", user.getUserId(), id);

        Shipment shipment = shipmentService.getShipment(id);
        verifyOwnership(user, shipment.getUserId());

        List<ShipmentTracking> trackingEvents = trackingService.getTrackingHistory(id);
        List<ShipmentTrackingResponse> response = trackingEvents.stream()
                .map(this::mapToTrackingResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/me")
    @Operation(summary = "Get my shipments", description = "Get all shipments for current user (requires JWT)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipments found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ShipmentResponse>> getMyShipments(HttpServletRequest request) {

        AuthenticatedUser user = getAuthenticatedUser(request);

        log.debug("User {} fetching their shipments", user.getUserId());

        List<Shipment> shipments = shipmentService.getShipmentsByUser(user.getUserId());
        List<ShipmentResponse> response = shipments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ========== Admin Endpoints (JWT Auth + ADMIN Role) ==========

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update shipment status", description = "Manually update shipment status (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShipmentStatusRequest request,
            HttpServletRequest httpRequest) {

        AuthenticatedUser user = getAuthenticatedUser(httpRequest);

        if (!user.isAdmin()) {
            throw new InvalidTokenException("Admin access required");
        }

        log.info("Admin {} updating shipment {} status to {}",
                user.getUserId(), id, request.status());

        ShipmentStatus newStatus = ShipmentStatus.valueOf(request.status());
        Shipment shipment = shipmentService.getShipment(id);
        ShipmentStatus previousStatus = shipment.getStatus();

        Shipment updatedShipment = shipmentService.updateShipmentStatus(
                id, newStatus, request.reason());

        // Publish appropriate event
        if (newStatus == ShipmentStatus.DELIVERED) {
            eventPublisher.publishShipmentDelivered(updatedShipment);
        } else if (newStatus == ShipmentStatus.CANCELLED) {
            eventPublisher.publishShipmentCancelled(updatedShipment, request.reason());
        } else {
            eventPublisher.publishShipmentUpdated(updatedShipment, previousStatus);
        }

        return ResponseEntity.ok(mapToResponse(updatedShipment));
    }

    @GetMapping
    @Operation(summary = "List all shipments", description = "Get all shipments with optional filters (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipments found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<List<ShipmentResponse>> listAllShipments(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        AuthenticatedUser user = getAuthenticatedUser(request);

        if (!user.isAdmin()) {
            throw new InvalidTokenException("Admin access required");
        }

        log.debug("Admin {} listing shipments with status filter: {}", user.getUserId(), status);

        List<Shipment> shipments;
        if (status != null) {
            ShipmentStatus shipmentStatus = ShipmentStatus.valueOf(status);
            shipments = shipmentService.getShipmentsByStatus(shipmentStatus);
        } else {
            shipments = shipmentService.getAllShipments();
        }

        List<ShipmentResponse> response = shipments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ========== Helper Methods ==========

    private void validateServiceSecret(String sharedSecret) {
        if (sharedSecret == null || !sharedSecret.equals(serviceProperties.sharedSecret())) {
            log.warn("Invalid service secret provided");
            throw new InvalidTokenException("Invalid service authentication");
        }
    }

    private AuthenticatedUser getAuthenticatedUser(HttpServletRequest request) {
        AuthenticatedUser user = (AuthenticatedUser) request.getAttribute(
                JwtValidationFilter.AUTHENTICATED_USER_ATTRIBUTE);

        if (user == null) {
            throw new InvalidTokenException("Authentication required");
        }

        return user;
    }

    private void verifyOwnership(AuthenticatedUser user, UUID resourceUserId) {
        if (!user.isAdmin() && !user.getUserId().equals(resourceUserId)) {
            log.warn("User {} attempted to access resource owned by {}",
                    user.getUserId(), resourceUserId);
            throw new InvalidTokenException("You do not have access to this shipment");
        }
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getShipmentId(),
                shipment.getOrderId(),
                shipment.getUserId(),
                shipment.getStatus().name(),
                shipment.getTrackingNumber(),
                shipment.getCarrier(),
                shipment.getShippingFee(),
                shipment.getCurrency(),
                shipment.getItemCount(),
                shipment.getPackageWeightKg(),
                shipment.getPackageDimensions(),
                shipment.getShippingAddress(),
                shipment.getEstimatedDeliveryDate(),
                shipment.getActualDeliveryDate(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }

    private ShipmentTrackingResponse mapToTrackingResponse(ShipmentTracking tracking) {
        return new ShipmentTrackingResponse(
                tracking.getId(),
                tracking.getStatus().name(),
                tracking.getLocation(),
                tracking.getDescription(),
                tracking.getEventTime(),
                tracking.getCreatedAt()
        );
    }
}
