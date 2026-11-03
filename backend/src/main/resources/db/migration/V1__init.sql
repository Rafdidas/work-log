create table teams (
    id   bigserial primary key,
    name varchar(100) not null
);

create table users (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    name          varchar(100) not null,
    team_id       bigint references teams (id),
    team_label    varchar(100) not null default '개발1팀',
    created_at    timestamptz  not null
);

create table work_items (
    id           bigserial primary key,
    user_id      bigint       not null references users (id),
    title        varchar(200) not null,
    tag          varchar(100),
    status       varchar(20)  not null,
    recurring    boolean      not null default false,
    show_in_plan boolean      not null default true,
    sort_order   int          not null,
    created_at   timestamptz  not null,
    started_at   date,
    completed_at date
);
create index idx_work_items_user_status on work_items (user_id, status);

create table day_logs (
    id        bigserial primary key,
    user_id   bigint        not null references users (id),
    date      date          not null,
    day_type  varchar(20)   not null,
    work_md   numeric(3, 2) not null,
    memo      text,
    unique (user_id, date)
);
create index idx_day_logs_user_date on day_logs (user_id, date);

create table day_entries (
    id           bigserial primary key,
    day_log_id   bigint        not null references day_logs (id) on delete cascade,
    work_item_id bigint        not null references work_items (id),
    weight       smallint      not null check (weight between 0 and 3),
    md           numeric(4, 2) not null,
    memo         text,
    unique (day_log_id, work_item_id)
);
