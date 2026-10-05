ALTER TABLE user_auth
    ADD COLUMN registration_completed BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE user_auth
    ALTER COLUMN registration_completed DROP DEFAULT;
