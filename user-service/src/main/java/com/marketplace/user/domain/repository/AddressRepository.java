package com.marketplace.user.domain.repository;

import com.marketplace.user.domain.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Address entity.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdAndIsDeletedFalse(UUID userId);

    Optional<Address> findByAddressIdAndUserIdAndIsDeletedFalse(UUID addressId, UUID userId);

    long countByUserIdAndIsDeletedFalse(UUID userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefaultShipping = false WHERE a.userId = :userId AND a.isDefaultShipping = true")
    void clearDefaultShipping(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefaultBilling = false WHERE a.userId = :userId AND a.isDefaultBilling = true")
    void clearDefaultBilling(@Param("userId") UUID userId);
}
