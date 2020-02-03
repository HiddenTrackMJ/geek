-- geek 后台表

-- user 用户信息

-- auto-generated definition
create table GEEK.USER
(
    ID       BIGINT auto_increment,
    NAME     VARCHAR(100) not null,
    PASSWORD VARCHAR(300) not null,
    AVATAR   VARCHAR(300),
    ROOMID   BIGINT,
    constraint USER_PK
        primary key (ID)
);

create unique index GEEK.USER_ID_UINDEX
    on GEEK.USER (ID);






-- room 房间信息