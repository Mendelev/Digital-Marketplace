-- Shipments table for tracking shipment lifecycle
CREATE TABLE shipments (
    shipment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    tracking_number VARCHAR(50) UNIQUE,
    carrier VARCHAR(20) NOT NULL DEFAULT 'MOCK',
    shipping_fee DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    package_weight_kg DECIMAL(10,3),
    package_dimensions VARCHAR(50),
    item_count INTEGER NOT NULL,
    shipping_address JSONB NOT NULL,
    estimated_delivery_date TIMESTAMP WITH TIME ZONE,
    actual_delivery_date TIMESTAMP WITH TIME ZONE,
    shipped_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_shipments_order_id UNIQUE (order_id),
    CONSTRAINT uk_shipments_tracking_number UNIQUE (tracking_number),
    CONSTRAINT ck_shipments_status CHECK (status IN (
        'PENDING', 'CREATED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY',
        'DELIVERED', 'CANCELLED', 'RETURNED'
    )),
    CONSTRAINT ck_shipments_shipping_fee CHECK (shipping_fee >= 0),
    CONSTRAINT ck_shipments_currency CHECK (LENGTH(currency) = 3),
    CONSTRAINT ck_shipments_item_count CHECK (item_count > 0),
    CONSTRAINT ck_shipments_package_weight CHECK (package_weight_kg IS NULL OR package_weight_kg > 0)
);

-- Indexes for common queries
CREATE INDEX idx_shipments_order_id ON shipments(order_id);
CREATE INDEX idx_shipments_user_id ON shipments(user_id);
CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_tracking_number ON shipments(tracking_number);
CREATE INDEX idx_shipments_created_at ON shipments(created_at);

-- Table comments
COMMENT ON TABLE shipments IS 'Shipment records for orders';
COMMENT ON COLUMN shipments.shipping_address IS 'JSONB snapshot of shipping address at time of shipment creation';
COMMENT ON COLUMN shipments.version IS 'Optimistic locking version';
COMMENT ON COLUMN shipments.estimated_delivery_date IS 'Estimated delivery date calculated at shipment creation';
COMMENT ON COLUMN shipments.actual_delivery_date IS 'Actual delivery date when shipment was delivered';
