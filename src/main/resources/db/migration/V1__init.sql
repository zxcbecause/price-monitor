create table tracked_product
(
    id                     bigserial primary key,
    marketplace            varchar(16)  not null,
    sku                    varchar(64)  not null,
    title                  varchar(512),
    target_price_kopecks   bigint,
    drop_threshold_percent integer,
    chat_id                bigint,
    last_price_kopecks     bigint,
    active                 boolean      not null default true,
    created_at             timestamptz  not null,
    last_checked_at        timestamptz,
    constraint uq_tracked_product unique (marketplace, sku)
);

create table price_snapshot
(
    id            bigserial primary key,
    product_id    bigint      not null references tracked_product (id) on delete cascade,
    price_kopecks bigint      not null,
    captured_at   timestamptz not null
);

create index idx_price_snapshot_product_time on price_snapshot (product_id, captured_at);
