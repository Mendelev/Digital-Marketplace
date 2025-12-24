CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider VARCHAR(20) NOT NULL DEFAULT 'MOCK',
    captured_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    refunded_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_payments_order_id UNIQUE (order_id),
    CONSTRAINT ck_payments_status CHECK (status IN ('INITIATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'VOIDED')),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_currency CHECK (LENGTH(currency) = 3),
    CONSTRAINT ck_payments_captured_amount CHECK (captured_amount >= 0 AND captured_amount <= amount),
    CONSTRAINT ck_payments_refunded_amount CHECK (refunded_amount >= 0 AND refunded_amount <= captured_amount)
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);

COMMENT ON TABLE payments IS 'Payment records and status';
COMMENT ON COLUMN payments.captured_amount IS 'Total amount captured (supports partial captures)';
COMMENT ON COLUMN payments.refunded_amount IS 'Total amount refunded (supports partial refunds)';
