-- V1: Create cart and cart_item tables

-- Cart table
CREATE TABLE cart (
    cart_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_cart_status CHECK (status IN ('ACTIVE', 'CHECKED_OUT', 'ABANDONED'))
);

-- Index for finding active cart by user
CREATE INDEX idx_cart_user_id_status ON cart(user_id, status);

-- Comments
COMMENT ON TABLE cart IS 'Shopping cart for each user';
COMMENT ON COLUMN cart.user_id IS 'Reference to user (no FK to avoid cross-service constraints)';
COMMENT ON COLUMN cart.status IS 'Cart status: ACTIVE (current), CHECKED_OUT (completed), ABANDONED (future use)';
COMMENT ON COLUMN cart.currency IS 'Cart currency code (ISO 4217)';

-- Cart Item table
CREATE TABLE cart_item (
    cart_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    title_snapshot VARCHAR(500) NOT NULL,
    unit_price_snapshot DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(cart_id) ON DELETE CASCADE,
    CONSTRAINT ck_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_unit_price_non_negative CHECK (unit_price_snapshot >= 0)
);

-- Index for finding items by cart
CREATE INDEX idx_cart_item_cart_id ON cart_item(cart_id);

-- Index for finding specific product in cart (for duplicate detection)
CREATE INDEX idx_cart_item_cart_product ON cart_item(cart_id, product_id);

-- Comments
COMMENT ON TABLE cart_item IS 'Items in shopping cart';
COMMENT ON COLUMN cart_item.product_id IS 'Reference to product in catalog service';
COMMENT ON COLUMN cart_item.sku IS 'Product SKU snapshot at time of add';
COMMENT ON COLUMN cart_item.title_snapshot IS 'Product title snapshot at time of add';
COMMENT ON COLUMN cart_item.unit_price_snapshot IS 'Product price snapshot at time of add';
COMMENT ON COLUMN cart_item.quantity IS 'Quantity of this product';
