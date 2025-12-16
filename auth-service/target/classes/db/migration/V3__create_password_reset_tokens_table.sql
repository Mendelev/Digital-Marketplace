-- Create password_reset_tokens table for password recovery flow
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash)
);

-- Create indexes for better query performance
CREATE INDEX idx_password_reset_tokens_email ON password_reset_tokens(email);
CREATE INDEX idx_password_reset_tokens_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_used_at ON password_reset_tokens(used_at);

-- Add comments
COMMENT ON TABLE password_reset_tokens IS 'Stores password reset tokens for account recovery';
COMMENT ON COLUMN password_reset_tokens.id IS 'Primary key (UUID)';
COMMENT ON COLUMN password_reset_tokens.email IS 'Email address requesting password reset';
COMMENT ON COLUMN password_reset_tokens.token_hash IS 'BCrypt hash of the reset token';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Token expiration timestamp (typically 1 hour)';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Timestamp when token was used (null if unused)';
