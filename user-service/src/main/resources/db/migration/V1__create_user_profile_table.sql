CREATE TABLE user_profile (
    user_id UUID PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(32),
    country_code VARCHAR(2),
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_user_profile_status CHECK (status IN ('INCOMPLETE', 'ACTIVE'))
);
