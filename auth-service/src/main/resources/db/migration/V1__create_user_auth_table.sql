CREATE TABLE user_auth (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_user_auth_email UNIQUE (email),
    CONSTRAINT chk_user_auth_role CHECK (role IN ('USER', 'ADMIN'))
);
