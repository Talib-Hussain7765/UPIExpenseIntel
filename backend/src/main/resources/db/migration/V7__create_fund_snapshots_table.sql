CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE fund_snapshots
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL references users(id) on delete cascade,
    total_balance   NUMERIC(19, 2) NOT NULL,
    reconciled_date DATE           NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_fund_snapshots_user_date
    ON fund_snapshots (user_id, reconciled_date DESC, created_at DESC);
