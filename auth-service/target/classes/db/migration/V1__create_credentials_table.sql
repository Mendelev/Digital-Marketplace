-- Create credentials table for storing user authentication data
CREATE TABLE credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_credentials_email UNIQUE (email),
    CONSTRAINT ck_credentials_status CHECK (status IN ('ACTIVE', 'LOCKED'))
);

-- Create indexes for better query performance
CREATE INDEX idx_credentials_user_id ON credentials(user_id);
CREATE INDEX idx_credentials_email ON credentials(email);
CREATE INDEX idx_credentials_status ON credentials(status);

-- Add comment to table
COMMENT ON TABLE credentials IS 'Stores user authentication credentials and login status';
COMMENT ON COLUMN credentials.id IS 'Primary key (UUID)';
COMMENT ON COLUMN credentials.user_id IS 'Reference to user in User Service (external)';
COMMENT ON COLUMN credentials.email IS 'User email address (unique)';
COMMENT ON COLUMN credentials.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN credentials.status IS 'Account status (ACTIVE or LOCKED)';
COMMENT ON COLUMN credentials.failed_login_count IS 'Counter for failed login attempts';
