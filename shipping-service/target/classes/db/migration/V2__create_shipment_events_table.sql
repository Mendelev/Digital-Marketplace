-- Shipment events table for event sourcing
CREATE TABLE shipment_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    shipment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    sequence_number BIGINT NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_shipment_events_shipment_id FOREIGN KEY (shipment_id)
        REFERENCES shipments(shipment_id) ON DELETE CASCADE
);

-- Indexes for querying events
CREATE INDEX idx_shipment_events_shipment_id ON shipment_events(shipment_id);
CREATE INDEX idx_shipment_events_event_id ON shipment_events(event_id);
CREATE INDEX idx_shipment_events_sequence_number ON shipment_events(sequence_number);
CREATE INDEX idx_shipment_events_published_at ON shipment_events(published_at);

-- Table comments
COMMENT ON TABLE shipment_events IS 'Event sourcing table for shipment lifecycle events';
COMMENT ON COLUMN shipment_events.event_id IS 'Unique identifier for the event (for idempotency)';
COMMENT ON COLUMN shipment_events.sequence_number IS 'Global sequence number for ordering events';
COMMENT ON COLUMN shipment_events.payload IS 'JSONB payload containing event data';
