create table users (
    id            uuid         primary key,
    username      varchar(50)  not null unique,
    email         varchar(254) not null unique,
    password_hash varchar(100) not null,
    created_at    timestamptz  not null default now()
);

create table local_pokemon (
    id                integer      primary key check (id > 0),
    name              varchar(100) not null unique,
    sprite_url        varchar(500),
    image_url         varchar(500),
    category          varchar(100),
    weight_hectograms integer      not null check (weight_hectograms >= 0),
    height_decimetres integer      not null check (height_decimetres >= 0),
    localized_name    varchar(100),
    region            varchar(50),
    habitat           varchar(50),
    notes             varchar(1000),
    version           bigint       not null default 0,
    synced_at         timestamptz  not null,
    updated_at        timestamptz  not null
);

create table local_pokemon_type (
    pokemon_id integer     not null references local_pokemon (id) on delete cascade,
    position   integer     not null,
    type       varchar(30) not null,
    primary key (pokemon_id, position)
);

create table local_pokemon_ability (
    pokemon_id integer      not null references local_pokemon (id) on delete cascade,
    position   integer      not null,
    ability    varchar(100) not null,
    primary key (pokemon_id, position)
);

create table local_pokemon_tag (
    pokemon_id integer     not null references local_pokemon (id) on delete cascade,
    tag        varchar(30) not null,
    primary key (pokemon_id, tag)
);
