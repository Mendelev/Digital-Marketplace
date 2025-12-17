package com.marketplace.user.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Address entity for shipping and billing addresses.
 */
@Entity
@Table(name = "addresses")
@EntityListeners(AuditingEntityListener.class)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "address_id")
    private UUID addressId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 20)
    private String zip;

    @Column(nullable = false, length = 200)
    private String street;

    @Column(nullable = false, length = 20)
    private String number;

    @Column(length = 200)
    private String complement;

    @Column(name = "is_default_shipping", nullable = false)
    private Boolean isDefaultShipping = false;

    @Column(name = "is_default_billing", nullable = false)
    private Boolean isDefaultBilling = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Address() {
    }

    public Address(UUID userId, String label, String country, String state, String city,
                   String zip, String street, String number, String complement) {
        this.userId = userId;
        this.label = label;
        this.country = country;
        this.state = state;
        this.city = city;
        this.zip = zip;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.isDefaultShipping = false;
        this.isDefaultBilling = false;
        this.isDeleted = false;
    }

    // Getters and Setters
    public UUID getAddressId() {
        return addressId;
    }

    public void setAddressId(UUID addressId) {
        this.addressId = addressId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public Boolean getIsDefaultShipping() {
        return isDefaultShipping;
    }

    public void setIsDefaultShipping(Boolean defaultShipping) {
        isDefaultShipping = defaultShipping;
    }

    public Boolean getIsDefaultBilling() {
        return isDefaultBilling;
    }

    public void setIsDefaultBilling(Boolean defaultBilling) {
        isDefaultBilling = defaultBilling;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
