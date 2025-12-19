-- Create stock_items table for mock inventory service
CREATE TABLE stock_items (
    sku VARCHAR(100) PRIMARY KEY,
    product_id UUID NOT NULL,
    available_qty INTEGER NOT NULL DEFAULT 0,
    reserved_qty INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_available_qty_non_negative CHECK (available_qty >= 0),
    CONSTRAINT ck_reserved_qty_non_negative CHECK (reserved_qty >= 0)
);

-- Create indexes for stock queries
CREATE INDEX idx_stock_items_product_id ON stock_items(product_id);

-- Create updated_at trigger for stock_items
CREATE TRIGGER update_stock_items_updated_at
    BEFORE UPDATE ON stock_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
