create extension if not exists pgcrypto;

create table users (
    id uuid primary key default gen_random_uuid(),
    email varchar(320) not null unique,
    password_hash varchar(255) not null,
    full_name varchar(160) not null,
    timezone varchar(64) not null default 'Asia/Kolkata',
    currency varchar(3) not null default 'INR',
    monthly_report_enabled boolean not null default false,
    monthly_report_day integer not null default 1,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_users_monthly_report_day check (monthly_report_day between 1 and 28)
);

create table bank_accounts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    bank_name varchar(120) not null,
    account_nickname varchar(120) not null,
    masked_account_number varchar(40),
    account_type varchar(40) not null default 'SAVINGS',
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table categories (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references users(id) on delete cascade,
    name varchar(120) not null,
    category_type varchar(30) not null,
    color varchar(20),
    icon varchar(80),
    display_order integer not null default 0,
    is_default boolean not null default false,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_categories_user_name unique (user_id, name)
);

create table statement_uploads (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    bank_account_id uuid not null references bank_accounts(id) on delete cascade,
    original_file_name varchar(255) not null,
    stored_file_path varchar(500),
    file_type varchar(30) not null,
    statement_month date not null,
    status varchar(40) not null,
    total_rows integer not null default 0,
    parsed_rows integer not null default 0,
    failed_rows integer not null default 0,
    error_message text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_statement_uploads_statement_month check (extract(day from statement_month) = 1)
);

create table transactions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    bank_account_id uuid not null references bank_accounts(id) on delete cascade,
    statement_upload_id uuid references statement_uploads(id) on delete set null,
    transaction_date date not null,
    value_date date,
    description varchar(1000) not null,
    raw_description varchar(1500) not null,
    amount numeric(19, 2) not null,
    transaction_type varchar(20) not null,
    payment_mode varchar(30) not null default 'UNKNOWN',
    merchant_name varchar(180),
    category_id uuid references categories(id) on delete set null,
    balance_after_transaction numeric(19, 2),
    reference_number varchar(120),
    is_recurring boolean not null default false,
    is_transfer boolean not null default false,
    is_ignored boolean not null default false,
    categorization_confidence numeric(5, 2),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table merchant_rules (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    keyword varchar(255) not null,
    match_type varchar(30) not null,
    category_id uuid not null references categories(id) on delete cascade,
    merchant_name varchar(180),
    priority integer not null default 100,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table monthly_reports (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    report_month date not null,
    total_income numeric(19, 2) not null default 0,
    total_expense numeric(19, 2) not null default 0,
    savings_amount numeric(19, 2) not null default 0,
    savings_rate numeric(7, 4) not null default 0,
    report_html_path varchar(500),
    report_pdf_path varchar(500),
    generated_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_monthly_reports_report_month check (extract(day from report_month) = 1),
    constraint uq_monthly_reports_user_month unique (user_id, report_month)
);

create table email_delivery_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    monthly_report_id uuid references monthly_reports(id) on delete set null,
    recipient_email varchar(320) not null,
    subject varchar(255) not null,
    status varchar(30) not null,
    provider_message_id varchar(255),
    error_message text,
    sent_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_bank_accounts_user_id on bank_accounts(user_id);
create index idx_categories_user_id on categories(user_id);
create index idx_statement_uploads_user_month on statement_uploads(user_id, statement_month);
create index idx_transactions_user_date on transactions(user_id, transaction_date);
create index idx_transactions_user_month_type on transactions(user_id, transaction_type, transaction_date);
create index idx_transactions_category_id on transactions(category_id);
create index idx_transactions_payment_mode on transactions(payment_mode);
create index idx_merchant_rules_user_priority on merchant_rules(user_id, priority);
create index idx_email_delivery_logs_user_id on email_delivery_logs(user_id);
