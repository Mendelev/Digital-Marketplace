package com.marketplace.user.service;

import com.marketplace.user.domain.model.Address;
import com.marketplace.user.domain.repository.AddressRepository;
import com.marketplace.user.dto.address.AddressResponse;
import com.marketplace.user.dto.address.CreateAddressRequest;
import com.marketplace.user.dto.address.UpdateAddressRequest;
import com.marketplace.user.util.EventLogger;
import com.marketplace.user.exception.ResourceNotFoundException;
import com.marketplace.user.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user addresses.
 */
@Service
public class AddressService {

    private static final Logger logger = LoggerFactory.getLogger(AddressService.class);

    private final AddressRepository addressRepository;
    private final EventLogger eventLogger;

    @Value("${user-service.max-addresses-per-user}")
    private int maxAddressesPerUser;

    public AddressService(AddressRepository addressRepository, EventLogger eventLogger) {
        this.addressRepository = addressRepository;
        this.eventLogger = eventLogger;
    }

    /**
     * Create a new address for a user.
     */
    @Transactional
    public AddressResponse createAddress(UUID userId, CreateAddressRequest request) {
        logger.info("Creating address for user: {}", userId);

        // Check max addresses limit
        long currentCount = addressRepository.countByUserIdAndIsDeletedFalse(userId);
        if (currentCount >= maxAddressesPerUser) {
            throw new ValidationException(
                    String.format("User has reached maximum limit of %d addresses", maxAddressesPerUser)
            );
        }

        Address address = new Address(
                userId,
                request.label(),
                request.country(),
                request.state(),
                request.city(),
                request.zip(),
                request.street(),
                request.number(),
                request.complement()
        );

        Address savedAddress = addressRepository.save(address);
        logger.info("Address created successfully: {}", savedAddress.getAddressId());

        eventLogger.logAddressCreated(savedAddress);

        return toResponse(savedAddress);
    }

    /**
     * Get an address by ID.
     */
    @Transactional(readOnly = true)
    public AddressResponse getAddress(UUID userId, UUID addressId) {
        logger.debug("Fetching address {} for user {}", addressId, userId);

        Address address = addressRepository.findByAddressIdAndUserIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        return toResponse(address);
    }

    /**
     * List all addresses for a user.
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UUID userId) {
        logger.debug("Listing addresses for user {}", userId);

        List<Address> addresses = addressRepository.findByUserIdAndIsDeletedFalse(userId);
        return addresses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing address.
     */
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request) {
        logger.info("Updating address {} for user {}", addressId, userId);

        Address address = addressRepository.findByAddressIdAndUserIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Store old values for event logging
        Address oldAddress = cloneAddress(address);

        // Update fields
        address.setLabel(request.label());
        address.setCountry(request.country());
        address.setState(request.state());
        address.setCity(request.city());
        address.setZip(request.zip());
        address.setStreet(request.street());
        address.setNumber(request.number());
        address.setComplement(request.complement());

        Address savedAddress = addressRepository.save(address);
        logger.info("Address updated successfully: {}", savedAddress.getAddressId());

        eventLogger.logAddressUpdated(oldAddress, savedAddress);

        return toResponse(savedAddress);
    }

    /**
     * Delete an address (soft delete).
     */
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        logger.info("Deleting address {} for user {}", addressId, userId);

        Address address = addressRepository.findByAddressIdAndUserIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Check if this is the last address
        long activeAddressCount = addressRepository.countByUserIdAndIsDeletedFalse(userId);
        if (activeAddressCount <= 1) {
            throw new ValidationException("Cannot delete the last address. At least one address must remain.");
        }

        // Soft delete
        address.setIsDeleted(true);
        
        // If this was a default address, clear the default flag
        if (address.getIsDefaultShipping()) {
            address.setIsDefaultShipping(false);
        }
        if (address.getIsDefaultBilling()) {
            address.setIsDefaultBilling(false);
        }

        addressRepository.save(address);
        logger.info("Address deleted successfully: {}", addressId);

        eventLogger.logAddressDeleted(address);
    }

    /**
     * Set an address as default shipping address.
     */
    @Transactional
    public AddressResponse setDefaultShipping(UUID userId, UUID addressId) {
        logger.info("Setting address {} as default shipping for user {}", addressId, userId);

        Address address = addressRepository.findByAddressIdAndUserIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Clear current default shipping
        addressRepository.clearDefaultShipping(userId);

        // Set new default
        address.setIsDefaultShipping(true);
        Address savedAddress = addressRepository.save(address);

        logger.info("Default shipping address set successfully: {}", addressId);
        eventLogger.logDefaultAddressChanged(savedAddress, "shipping");

        return toResponse(savedAddress);
    }

    /**
     * Set an address as default billing address.
     */
    @Transactional
    public AddressResponse setDefaultBilling(UUID userId, UUID addressId) {
        logger.info("Setting address {} as default billing for user {}", addressId, userId);

        Address address = addressRepository.findByAddressIdAndUserIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Clear current default billing
        addressRepository.clearDefaultBilling(userId);

        // Set new default
        address.setIsDefaultBilling(true);
        Address savedAddress = addressRepository.save(address);

        logger.info("Default billing address set successfully: {}", addressId);
        eventLogger.logDefaultAddressChanged(savedAddress, "billing");

        return toResponse(savedAddress);
    }

    /**
     * Convert Address entity to AddressResponse DTO.
     */
    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getAddressId(),
                address.getUserId(),
                address.getLabel(),
                address.getCountry(),
                address.getState(),
                address.getCity(),
                address.getZip(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getIsDefaultShipping(),
                address.getIsDefaultBilling(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    /**
     * Clone an address for event logging.
     */
    private Address cloneAddress(Address address) {
        Address clone = new Address(
                address.getUserId(),
                address.getLabel(),
                address.getCountry(),
                address.getState(),
                address.getCity(),
                address.getZip(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement()
        );
        clone.setAddressId(address.getAddressId());
        clone.setIsDefaultShipping(address.getIsDefaultShipping());
        clone.setIsDefaultBilling(address.getIsDefaultBilling());
        return clone;
    }
}
