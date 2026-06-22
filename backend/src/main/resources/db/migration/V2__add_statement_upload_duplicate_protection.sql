alter table statement_uploads
    add column file_sha256 varchar(64);

create unique index uq_statement_uploads_file_sha256
    on statement_uploads(user_id, bank_account_id, statement_month, file_sha256)
    where file_sha256 is not null;
