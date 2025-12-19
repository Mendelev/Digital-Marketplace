package com.marketplace.inventory.controller;

import com.marketplace.inventory.dto.MessageResponse;
import com.marketplace.inventory.service.ReservationService;
import com.marketplace.shared.dto.inventory.ReservationResponse;
import com.marketplace.shared.dto.inventory.ReserveStockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory Service-to-Service", description = "Service-to-service APIs for stock reservations (requires X-Service-Secret header)")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final ReservationService reservationService;

    public InventoryController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @Operation(summary = "Reserve stock for an order", description = "Called by Order Service to reserve stock")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Stock reserved successfully"),
        @ApiResponse(responseCode = "409", description = "Insufficient stock or duplicate reservation"),
        @ApiResponse(responseCode = "404", description = "Invalid SKU"),
        @ApiResponse(responseCode = "503", description = "Catalog service unavailable")
    })
    public ResponseEntity<ReservationResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {

        log.info("Reservation request received for order: {}", request.orderId());

        ReservationResponse response = reservationService.reserveStock(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/reservations/{reservationId}/confirm")
    @Operation(summary = "Confirm stock reservation", description = "Called by Order Service after successful payment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation confirmed"),
        @ApiResponse(responseCode = "404", description = "Reservation not found"),
        @ApiResponse(responseCode = "410", description = "Reservation expired"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation state")
    })
    public ResponseEntity<MessageResponse> confirmReservation(@PathVariable UUID reservationId) {

        log.info("Confirm reservation request: {}", reservationId);

        reservationService.confirmReservation(reservationId);

        return ResponseEntity.ok(new MessageResponse("Reservation confirmed successfully"));
    }

    @PutMapping("/reservations/{reservationId}/release")
    @Operation(summary = "Release stock reservation", description = "Called by Order Service on cancellation or payment failure")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation released"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<MessageResponse> releaseReservation(
            @PathVariable UUID reservationId,
            @RequestParam(required = false, defaultValue = "Order cancelled") String reason) {

        log.info("Release reservation request: {} (reason: {})", reservationId, reason);

        reservationService.releaseReservation(reservationId, reason);

        return ResponseEntity.ok(new MessageResponse("Reservation released successfully"));
    }

    @GetMapping("/reservations/{reservationId}")
    @Operation(summary = "Get reservation details", description = "Retrieve reservation information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservation found"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID reservationId) {

        log.info("Get reservation request: {}", reservationId);

        ReservationResponse response = reservationService.getReservation(reservationId);

        return ResponseEntity.ok(response);
    }
}
