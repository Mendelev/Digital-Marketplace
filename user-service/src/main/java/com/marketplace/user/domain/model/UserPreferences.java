package com.marketplace.user.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User preferences entity for notification settings.
 */
@Entity
@Table(name = "user_preferences")
@EntityListeners(AuditingEntityListener.class)
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_notifications", nullable = false)
    private Boolean emailNotifications = true;

    @Column(name = "sms_notifications", nullable = false)
    private Boolean smsNotifications = false;

    @Column(name = "order_updates", nullable = false)
    private Boolean orderUpdates = true;

    @Column(name = "promotional_emails", nullable = false)
    private Boolean promotionalEmails = true;

    @Column(name = "newsletter_subscription", nullable = false)
    private Boolean newsletterSubscription = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserPreferences() {
    }

    public UserPreferences(UUID userId) {
        this.userId = userId;
        this.emailNotifications = true;
        this.smsNotifications = false;
        this.orderUpdates = true;
        this.promotionalEmails = true;
        this.newsletterSubscription = false;
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getSmsNotifications() {
        return smsNotifications;
    }

    public void setSmsNotifications(Boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
    }

    public Boolean getOrderUpdates() {
        return orderUpdates;
    }

    public void setOrderUpdates(Boolean orderUpdates) {
        this.orderUpdates = orderUpdates;
    }

    public Boolean getPromotionalEmails() {
        return promotionalEmails;
    }

    public void setPromotionalEmails(Boolean promotionalEmails) {
        this.promotionalEmails = promotionalEmails;
    }

    public Boolean getNewsletterSubscription() {
        return newsletterSubscription;
    }

    public void setNewsletterSubscription(Boolean newsletterSubscription) {
        this.newsletterSubscription = newsletterSubscription;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
