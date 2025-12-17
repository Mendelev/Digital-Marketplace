package com.marketplace.auth.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an orphaned user that needs compensating deletion from User Service.
 * This occurs when Auth Service fails to save credentials after User Service creates the user.
 */
@Entity
@Table(name = "orphaned_users")
public class OrphanedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "email")
    private String email;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrphanedUserStatus status = OrphanedUserStatus.PENDING;

    protected OrphanedUser() {
    }

    public OrphanedUser(UUID userId, String email) {
        this.userId = userId;
        this.email = email;
        this.failedAt = LocalDateTime.now();
        this.retryCount = 0;
        this.status = OrphanedUserStatus.PENDING;
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = OrphanedUserStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = OrphanedUserStatus.FAILED;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getLastRetryAt() {
        return lastRetryAt;
    }

    public void setLastRetryAt(LocalDateTime lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
    }

    public OrphanedUserStatus getStatus() {
        return status;
    }

    public void setStatus(OrphanedUserStatus status) {
        this.status = status;
    }

    public enum OrphanedUserStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
