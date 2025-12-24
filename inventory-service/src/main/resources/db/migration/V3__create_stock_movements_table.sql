-- Create stock_movements table for audit trail
CREATE TABLE stock_movements (
    movement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(100) NOT NULL,
    movement_type VARCHAR(30) NOT NULL CHECK (movement_type IN
        ('RESERVE', 'CONFIRM', 'RELEASE', 'RESTOCK', 'ADJUSTMENT', 'LOW_STOCK_CORRECTION')),
    quantity INTEGER NOT NULL,
    previous_available_qty INTEGER NOT NULL,
    new_available_qty INTEGER NOT NULL,
    previous_reserved_qty INTEGER NOT NULL,
    new_reserved_qty INTEGER NOT NULL,
    reservation_id UUID,
    reason TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movement_stock_item FOREIGN KEY (sku)
        REFERENCES stock_items(sku)
);

-- Indexes for audit queries
CREATE INDEX idx_stock_movements_sku ON stock_movements(sku);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at DESC);
CREATE INDEX idx_stock_movements_reservation ON stock_movements(reservation_id)
    WHERE reservation_id IS NOT NULL;
CREATE INDEX idx_stock_movements_type ON stock_movements(movement_type);

-- Comments
COMMENT ON TABLE stock_movements IS 'Audit trail for all stock quantity changes';
COMMENT ON COLUMN stock_movements.movement_type IS 'Type of stock operation performed';
COMMENT ON COLUMN stock_movements.quantity IS 'Quantity affected by this movement';
COMMENT ON COLUMN stock_movements.reservation_id IS 'Associated reservation if applicable';
COMMENT ON COLUMN stock_movements.reason IS 'Human-readable reason for the movement';
COMMENT ON COLUMN stock_movements.created_by IS 'User, system, or service that initiated the movement';
