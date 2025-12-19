CREATE TABLE product_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    product_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    sequence_number BIGINT NOT NULL,
    payload JSONB NOT NULL,
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_event FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_events_product ON product_events(product_id);
CREATE INDEX idx_product_events_sequence ON product_events(sequence_number);
CREATE UNIQUE INDEX idx_product_events_idempotency ON product_events(event_id);
