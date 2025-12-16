-- V1: Create users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for email lookups
CREATE INDEX idx_users_email ON users(email);

-- Comments
COMMENT ON TABLE users IS 'User profiles';
COMMENT ON COLUMN users.user_id IS 'User ID from Auth Service';
COMMENT ON COLUMN users.email IS 'User email (copy from Auth Service)';
COMMENT ON COLUMN users.name IS 'User full name';
COMMENT ON COLUMN users.phone IS 'Optional phone number in E.164 format';
