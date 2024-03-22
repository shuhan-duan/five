create database five_online;

use five_online;

create table game_history
(
    id          bigint auto_increment
        primary key,
    black_id    bigint            not null,
    white_id    bigint            not null,
    begin_time  datetime          null,
    end_time    datetime          null,
    game_result tinyint default 2 not null,
    room_id     mediumtext        null
);

# 用户表，存储用户相关信息

create table user
(
    id                    bigint auto_increment
        primary key,
    username              varchar(15)                                                                                                        not null,
    password              varchar(15)                                                                                                        not null,
    game_total_counts     int          default 0                                                                                             not null,
    game_success_counts   int          default 0                                                                                             not null,
    game_fail_counts      int          default 0                                                                                             not null,
    game_dead_heat_counts int          default 0                                                                                             not null,
    deleted               tinyint      default 0                                                                                             not null,
    constraint username
        unique (username)
);
