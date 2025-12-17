-- Create orphaned_users table for tracking users that need compensating deletion
CREATE TABLE orphaned_users (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    email VARCHAR(255),
    failed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

-- Create index for efficient querying of pending orphaned users
CREATE INDEX idx_orphaned_users_status ON orphaned_users(status) WHERE status = 'PENDING';
CREATE INDEX idx_orphaned_users_failed_at ON orphaned_users(failed_at);

-- Add comments
COMMENT ON TABLE orphaned_users IS 'Tracks orphaned users in User Service that need compensating deletion';
COMMENT ON COLUMN orphaned_users.user_id IS 'UUID of the orphaned user in User Service';
COMMENT ON COLUMN orphaned_users.email IS 'Email of the orphaned user for reference';
COMMENT ON COLUMN orphaned_users.failed_at IS 'Timestamp when the orphaned user was detected';
COMMENT ON COLUMN orphaned_users.retry_count IS 'Number of deletion retry attempts';
COMMENT ON COLUMN orphaned_users.last_retry_at IS 'Timestamp of the last retry attempt';
COMMENT ON COLUMN orphaned_users.status IS 'Status: PENDING, COMPLETED, or FAILED';
