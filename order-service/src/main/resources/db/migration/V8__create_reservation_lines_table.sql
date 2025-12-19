-- Create reservation_lines table for reservation line items
CREATE TABLE reservation_lines (
    reservation_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT fk_reservation_line_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(reservation_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reservation_line_stock_item
        FOREIGN KEY (sku)
        REFERENCES stock_items(sku)
        ON DELETE RESTRICT,
    CONSTRAINT ck_reservation_line_quantity CHECK (quantity > 0)
);

-- Create indexes for reservation line queries
CREATE INDEX idx_reservation_lines_reservation_id ON reservation_lines(reservation_id);
CREATE INDEX idx_reservation_lines_sku ON reservation_lines(sku);
