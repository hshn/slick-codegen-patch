create table users
(
    id         varchar(255) primary key,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp on update current_timestamp
) engine = InnoDB
  default charset = utf8mb4;

create table posts
(
    id         varchar(255) primary key,
    user_id    varchar(255) not null,
    title      varchar(255) not null,
    content    text         not null,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp on update current_timestamp,
    constraint foreign key (user_id) references users (id)
) engine = InnoDB
  default charset = utf8mb4;

create table tags
(
    id         varchar(255) primary key,
    name       varchar(255) not null,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp on update current_timestamp
) engine = InnoDB
  default charset = utf8mb4;

create table post_tags
(
    id         varchar(255) primary key,
    post_id    varchar(255) not null,
    tag_id     varchar(255) not null,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp on update current_timestamp,
    constraint foreign key (post_id) references posts (id),
    constraint foreign key (tag_id) references tags (id)
) engine = InnoDB
  default charset = utf8mb4;
