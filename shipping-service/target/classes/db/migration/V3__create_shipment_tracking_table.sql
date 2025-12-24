-- Shipment tracking table for detailed tracking history
CREATE TABLE shipment_tracking (
    id BIGSERIAL PRIMARY KEY,
    shipment_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    location VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_shipment_tracking_shipment_id FOREIGN KEY (shipment_id)
        REFERENCES shipments(shipment_id) ON DELETE CASCADE,
    CONSTRAINT ck_shipment_tracking_status CHECK (status IN (
        'PENDING', 'CREATED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY',
        'DELIVERED', 'CANCELLED', 'RETURNED'
    ))
);

-- Indexes for querying tracking history
CREATE INDEX idx_shipment_tracking_shipment_id ON shipment_tracking(shipment_id);
CREATE INDEX idx_shipment_tracking_event_time ON shipment_tracking(event_time);

-- Table comments
COMMENT ON TABLE shipment_tracking IS 'Detailed tracking history for shipments';
COMMENT ON COLUMN shipment_tracking.location IS 'Physical location of the package at the time of the event';
COMMENT ON COLUMN shipment_tracking.event_time IS 'When the tracking event occurred (not when it was recorded)';
