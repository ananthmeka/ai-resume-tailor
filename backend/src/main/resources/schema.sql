create table if not exists beta_usage (
    user_id varchar(100) not null,
    bucket varchar(32) not null,
    request_count integer not null,
    updated_at timestamp not null,
    primary key (user_id, bucket)
);
