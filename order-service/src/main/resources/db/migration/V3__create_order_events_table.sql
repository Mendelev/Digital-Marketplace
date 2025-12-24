-- Create order_events table for event sourcing
CREATE TABLE order_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    order_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    sequence_number BIGINT NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_event_order
        FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE
);

-- Create indexes for event queries
CREATE INDEX idx_order_events_order_id ON order_events(order_id);
CREATE INDEX idx_order_events_sequence_number ON order_events(sequence_number);
CREATE UNIQUE INDEX idx_order_events_event_id ON order_events(event_id);
CREATE INDEX idx_order_events_event_type ON order_events(event_type);
