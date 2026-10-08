alter table statement_uploads
    add column skipped_rows integer not null default 0,
    add column duplicate_rows integer not null default 0;
