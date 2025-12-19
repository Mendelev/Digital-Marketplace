-- Create reservations table
CREATE TABLE reservations (
    reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create reservation_lines table
CREATE TABLE reservation_lines (
    reservation_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),

    CONSTRAINT fk_reservation FOREIGN KEY (reservation_id)
        REFERENCES reservations(reservation_id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_item FOREIGN KEY (sku)
        REFERENCES stock_items(sku)
);

-- Indexes for performance
CREATE INDEX idx_reservations_order_id ON reservations(order_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_expires_at ON reservations(expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_reservation_lines_reservation ON reservation_lines(reservation_id);
CREATE INDEX idx_reservation_lines_sku ON reservation_lines(sku);

-- Comments
COMMENT ON TABLE reservations IS 'Stock reservations for pending orders';
COMMENT ON COLUMN reservations.order_id IS 'Reference to order in Order Service';
COMMENT ON COLUMN reservations.status IS 'Reservation lifecycle status';
COMMENT ON COLUMN reservations.expires_at IS 'TTL expiration timestamp';

COMMENT ON TABLE reservation_lines IS 'Line items for stock reservations';
COMMENT ON COLUMN reservation_lines.sku IS 'SKU being reserved';
COMMENT ON COLUMN reservation_lines.quantity IS 'Quantity reserved';
