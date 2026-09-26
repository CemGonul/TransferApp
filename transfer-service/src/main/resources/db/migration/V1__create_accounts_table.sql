CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    account_number VARCHAR(13) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_account_number UNIQUE (account_number),
    CONSTRAINT uk_user_currency UNIQUE (user_id, currency),
    CONSTRAINT chk_account_currency CHECK (currency IN ('PLN', 'GBP', 'EUR', 'USD')),
    CONSTRAINT chk_account_balance CHECK (balance >= 0)
);
