CREATE INDEX idx_transfers_sender_created_at
    ON transfers (sender_account_id, created_at DESC);

CREATE INDEX idx_transfers_receiver_created_at
    ON transfers (receiver_account_id, created_at DESC);
