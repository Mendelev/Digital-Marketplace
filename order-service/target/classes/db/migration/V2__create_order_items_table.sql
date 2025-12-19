-- Create order_items table
CREATE TABLE order_items (
    order_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    title_snapshot VARCHAR(500) NOT NULL,
    unit_price_snapshot DECIMAL(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total_amount DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_line_amounts_non_negative CHECK (
        unit_price_snapshot >= 0 AND
        line_total_amount >= 0
    )
);

-- Create indexes for common queries
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_order_items_sku ON order_items(sku);
