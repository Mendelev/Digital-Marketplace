-- V3: Create addresses table
CREATE TABLE addresses (
    address_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    label VARCHAR(50) NOT NULL,
    country VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    zip VARCHAR(20) NOT NULL,
    street VARCHAR(200) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(200),
    is_default_shipping BOOLEAN NOT NULL DEFAULT FALSE,
    is_default_billing BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_addresses_user_id ON addresses(user_id);
CREATE INDEX idx_addresses_user_id_is_deleted ON addresses(user_id, is_deleted);
CREATE INDEX idx_addresses_default_shipping ON addresses(user_id, is_default_shipping) WHERE is_default_shipping = TRUE;
CREATE INDEX idx_addresses_default_billing ON addresses(user_id, is_default_billing) WHERE is_default_billing = TRUE;

-- Comments
COMMENT ON TABLE addresses IS 'User shipping and billing addresses';
COMMENT ON COLUMN addresses.label IS 'Address label (e.g., Home, Work)';
COMMENT ON COLUMN addresses.is_default_shipping IS 'Default shipping address flag';
COMMENT ON COLUMN addresses.is_default_billing IS 'Default billing address flag';
COMMENT ON COLUMN addresses.is_deleted IS 'Soft delete flag';
