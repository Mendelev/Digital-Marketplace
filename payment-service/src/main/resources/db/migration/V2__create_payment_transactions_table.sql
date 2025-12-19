CREATE TABLE payment_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider_reference VARCHAR(100),
    error_message VARCHAR(500),
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_transactions_payment_id FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id) ON DELETE CASCADE,
    CONSTRAINT ck_payment_transactions_type CHECK (type IN ('AUTHORIZE', 'CAPTURE', 'REFUND', 'VOID')),
    CONSTRAINT ck_payment_transactions_status CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT ck_payment_transactions_amount CHECK (amount > 0),
    CONSTRAINT uk_payment_transactions_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions(payment_id);
CREATE INDEX idx_payment_transactions_created_at ON payment_transactions(created_at);
CREATE INDEX idx_payment_transactions_idempotency_key ON payment_transactions(idempotency_key);

COMMENT ON TABLE payment_transactions IS 'Payment transaction history';
