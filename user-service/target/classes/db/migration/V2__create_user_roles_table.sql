-- V2: Create user_roles table
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_role CHECK (role IN ('CUSTOMER', 'SELLER', 'ADMIN'))
);

-- Index for role-based queries
CREATE INDEX idx_user_roles_role ON user_roles(role);

-- Comments
COMMENT ON TABLE user_roles IS 'User roles (many-to-many relationship)';
COMMENT ON COLUMN user_roles.role IS 'Role: CUSTOMER, SELLER, or ADMIN';
