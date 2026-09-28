CREATE INDEX idx_idempotency_requests_expires_at
    ON idempotency_requests (expires_at);
