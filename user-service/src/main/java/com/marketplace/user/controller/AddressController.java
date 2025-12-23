package com.marketplace.user.controller;

import com.marketplace.user.config.AuthServiceProperties;
import com.marketplace.user.dto.address.AddressResponse;
import com.marketplace.user.dto.address.CreateAddressRequest;
import com.marketplace.user.dto.address.UpdateAddressRequest;
import com.marketplace.user.security.AuthenticatedUser;
import com.marketplace.user.security.CurrentUser;
import com.marketplace.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for address management.
 */
@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Address Management", description = "APIs for managing user addresses")
public class AddressController {

    private static final Logger logger = LoggerFactory.getLogger(AddressController.class);
    private static final String SERVICE_SECRET_HEADER = "X-Service-Secret";

    private final AddressService addressService;
    private final AuthServiceProperties authServiceProperties;

    public AddressController(AddressService addressService, AuthServiceProperties authServiceProperties) {
        this.addressService = addressService;
        this.authServiceProperties = authServiceProperties;
    }

    /**
     * Create a new address for the authenticated user.
     */
    @PostMapping
    @Operation(summary = "Create address", description = "Create a new address for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Maximum addresses limit reached")
    })
    public ResponseEntity<AddressResponse> createAddress(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateAddressRequest request) {

        logger.info("Create address request for user: {}", authenticatedUser.getUserId());

        AddressResponse response = addressService.createAddress(authenticatedUser.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a specific address by ID.
     */
    @GetMapping("/{addressId}")
    @Operation(summary = "Get address", description = "Get a specific address by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressResponse> getAddress(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @Parameter(description = "Address ID", required = true)
            @PathVariable UUID addressId) {

        logger.debug("Get address request: {} for user: {}", addressId, authenticatedUser.getUserId());

        AddressResponse response = addressService.getAddress(authenticatedUser.getUserId(), addressId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get address by ID - internal endpoint for service-to-service calls.
     * Requires shared secret header.
     */
    @GetMapping("/internal/{addressId}")
    @Operation(summary = "Get address (Internal)", description = "Get address by ID for internal service calls")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Invalid shared secret"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressResponse> getAddressInternal(
            @Parameter(description = "Address ID", required = true)
            @PathVariable UUID addressId,
            @RequestHeader(value = SERVICE_SECRET_HEADER, required = false) String sharedSecret) {

        if (sharedSecret == null || !sharedSecret.equals(authServiceProperties.sharedSecret())) {
            logger.error("Invalid or missing shared secret in internal get address request");
            throw new com.marketplace.user.exception.InvalidSharedSecretException("Invalid service authentication");
        }

        AddressResponse response = addressService.getAddressInternal(addressId);
        return ResponseEntity.ok(response);
    }

    /**
     * List all addresses for the authenticated user.
     */
    @GetMapping
    @Operation(summary = "List addresses", description = "Get all addresses for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<AddressResponse>> listAddresses(
            @CurrentUser AuthenticatedUser authenticatedUser) {

        logger.debug("List addresses request for user: {}", authenticatedUser.getUserId());

        List<AddressResponse> responses = addressService.listAddresses(authenticatedUser.getUserId());
        return ResponseEntity.ok(responses);
    }

    /**
     * Update an existing address.
     */
    @PutMapping("/{addressId}")
    @Operation(summary = "Update address", description = "Update an existing address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressResponse> updateAddress(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @Parameter(description = "Address ID", required = true)
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateAddressRequest request) {

        logger.info("Update address request: {} for user: {}", addressId, authenticatedUser.getUserId());

        AddressResponse response = addressService.updateAddress(
                authenticatedUser.getUserId(),
                addressId,
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an address (soft delete).
     */
    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete address", description = "Delete an address (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "422", description = "Cannot delete last address")
    })
    public ResponseEntity<Void> deleteAddress(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @Parameter(description = "Address ID", required = true)
            @PathVariable UUID addressId) {

        logger.info("Delete address request: {} for user: {}", addressId, authenticatedUser.getUserId());

        addressService.deleteAddress(authenticatedUser.getUserId(), addressId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set an address as default shipping address.
     */
    @PatchMapping("/{addressId}/default-shipping")
    @Operation(summary = "Set default shipping", description = "Set an address as default shipping address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default shipping address set successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressResponse> setDefaultShipping(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @Parameter(description = "Address ID", required = true)
            @PathVariable UUID addressId) {

        logger.info("Set default shipping address: {} for user: {}", addressId, authenticatedUser.getUserId());

        AddressResponse response = addressService.setDefaultShipping(
                authenticatedUser.getUserId(),
                addressId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Set an address as default billing address.
     */
    @PatchMapping("/{addressId}/default-billing")
    @Operation(summary = "Set default billing", description = "Set an address as default billing address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default billing address set successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressResponse> setDefaultBilling(
            @CurrentUser AuthenticatedUser authenticatedUser,
            @Parameter(description = "Address ID", required = true)
            @PathVariable UUID addressId) {

        logger.info("Set default billing address: {} for user: {}", addressId, authenticatedUser.getUserId());

        AddressResponse response = addressService.setDefaultBilling(
                authenticatedUser.getUserId(),
                addressId
        );
        return ResponseEntity.ok(response);
    }
}
