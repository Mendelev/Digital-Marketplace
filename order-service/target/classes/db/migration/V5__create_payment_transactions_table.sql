-- Create payment_transactions table for audit trail
CREATE TABLE payment_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    provider_reference VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_transaction_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_transaction_type CHECK (type IN (
        'AUTHORIZE',
        'CAPTURE',
        'REFUND',
        'VOID'
    )),
    CONSTRAINT ck_transaction_status CHECK (status IN (
        'SUCCESS',
        'FAILED',
        'PENDING'
    )),
    CONSTRAINT ck_transaction_amount CHECK (amount >= 0)
);

-- Create indexes for transaction queries
CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions(payment_id);
CREATE INDEX idx_payment_transactions_type ON payment_transactions(type);
CREATE INDEX idx_payment_transactions_created_at ON payment_transactions(created_at DESC);
