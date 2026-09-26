CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    sender_account_id UUID NOT NULL,
    receiver_account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT fk_transfers_sender_account
        FOREIGN KEY (sender_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transfers_receiver_account
        FOREIGN KEY (receiver_account_id) REFERENCES accounts (id),
    CONSTRAINT chk_transfer_different_accounts
        CHECK (sender_account_id <> receiver_account_id),
    CONSTRAINT chk_transfer_amount CHECK (amount > 0),
    CONSTRAINT chk_transfer_currency CHECK (currency IN ('PLN', 'GBP', 'EUR', 'USD')),
    CONSTRAINT chk_transfer_status CHECK (status IN ('PENDING', 'COMPLETED'))
);
