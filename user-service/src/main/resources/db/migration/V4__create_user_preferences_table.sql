-- V4: Create user_preferences table
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY,
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    sms_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    order_updates BOOLEAN NOT NULL DEFAULT TRUE,
    promotional_emails BOOLEAN NOT NULL DEFAULT TRUE,
    newsletter_subscription BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Comments
COMMENT ON TABLE user_preferences IS 'User notification preferences';
COMMENT ON COLUMN user_preferences.email_notifications IS 'Enable email notifications';
COMMENT ON COLUMN user_preferences.sms_notifications IS 'Enable SMS notifications';
COMMENT ON COLUMN user_preferences.order_updates IS 'Order update notifications (always true)';
COMMENT ON COLUMN user_preferences.promotional_emails IS 'Promotional email opt-in';
COMMENT ON COLUMN user_preferences.newsletter_subscription IS 'Newsletter subscription';
