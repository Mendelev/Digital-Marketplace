CREATE TABLE product_price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id UUID NOT NULL,
    old_price DECIMAL(10, 2) NOT NULL,
    new_price DECIMAL(10, 2) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID NOT NULL,
    CONSTRAINT fk_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_price_history_product ON product_price_history(product_id);
CREATE INDEX idx_price_history_changed_at ON product_price_history(changed_at DESC);
