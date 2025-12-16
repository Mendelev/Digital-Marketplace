package com.marketplace.auth.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing user authentication credentials.
 * Stores email, password hash, and account status.
 */
@Entity
@Table(name = "credentials")
@EntityListeners(AuditingEntityListener.class)
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CredentialStatus status;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Credential() {
        this.status = CredentialStatus.ACTIVE;
    }

    public Credential(UUID userId, String email, String passwordHash) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = CredentialStatus.ACTIVE;
        this.failedLoginCount = 0;
    }

    // Business methods
    public void incrementFailedLoginCount() {
        this.failedLoginCount++;
        if (this.failedLoginCount >= 5) {
            this.status = CredentialStatus.LOCKED;
        }
    }

    public void resetFailedLoginCount() {
        this.failedLoginCount = 0;
    }

    public void lock() {
        this.status = CredentialStatus.LOCKED;
    }

    public void activate() {
        this.status = CredentialStatus.ACTIVE;
        this.failedLoginCount = 0;
    }

    public boolean isLocked() {
        return this.status == CredentialStatus.LOCKED;
    }

    public boolean isActive() {
        return this.status == CredentialStatus.ACTIVE;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public CredentialStatus getStatus() {
        return status;
    }

    public void setStatus(CredentialStatus status) {
        this.status = status;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
