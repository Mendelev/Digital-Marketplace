-- Create stock_items table
CREATE TABLE stock_items (
    sku VARCHAR(100) PRIMARY KEY,
    product_id UUID NOT NULL,
    available_qty INTEGER NOT NULL DEFAULT 0 CHECK (available_qty >= 0),
    reserved_qty INTEGER NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
    low_stock_threshold INTEGER NOT NULL DEFAULT 10,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_stock_items_product_id ON stock_items(product_id);
CREATE INDEX idx_stock_items_low_stock ON stock_items(available_qty)
    WHERE available_qty <= low_stock_threshold;

-- Comments
COMMENT ON TABLE stock_items IS 'Inventory stock levels per SKU';
COMMENT ON COLUMN stock_items.sku IS 'Stock Keeping Unit identifier';
COMMENT ON COLUMN stock_items.product_id IS 'Reference to product in Catalog Service';
COMMENT ON COLUMN stock_items.available_qty IS 'Current available quantity for sale';
COMMENT ON COLUMN stock_items.reserved_qty IS 'Quantity reserved for pending orders';
COMMENT ON COLUMN stock_items.low_stock_threshold IS 'Threshold for low stock alerts';
COMMENT ON COLUMN stock_items.version IS 'Optimistic locking version for concurrency control';
