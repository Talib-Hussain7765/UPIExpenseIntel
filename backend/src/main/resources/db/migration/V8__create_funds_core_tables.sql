CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE funds
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL references users(id) on delete cascade,
    name       VARCHAR(255)   NOT NULL,
    balance    NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT uk_funds_user_name
        UNIQUE (user_id, name)
);

CREATE TABLE fund_transactions
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          uuid NOT NULL references users(id) on delete cascade,
    fund_id          UUID NOT NULL references funds(id) on delete cascade,
    amount           NUMERIC(19, 2) NOT NULL,
    type             VARCHAR(20)    NOT NULL,
    transaction_date DATE           NOT NULL,
    note             VARCHAR(500),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_funds_user_id
    ON funds (user_id);

CREATE INDEX idx_fund_transactions_user_fund_date
    ON fund_transactions (user_id, fund_id, transaction_date DESC);
