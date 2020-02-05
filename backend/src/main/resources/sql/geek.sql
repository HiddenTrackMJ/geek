-- geek 后台表

-- user 用户信息
create schema GEEK;

-- auto-generated definition
CREATE SEQUENCE user_id_seq START WITH 1000;
create table GEEK.USER
(
    ID       BIGINT DEFAULT user_id_seq.nextval PRIMARY KEY,
    NAME     VARCHAR(100) not null,
    PASSWORD VARCHAR(300) not null,
    AVATAR   VARCHAR(300)
);

create unique index GEEK.USER_ID_UINDEX
    on GEEK.USER (ID);

CREATE SEQUENCE room_id_seq START WITH 1000;
create table GEEK.ROOM
(
    ID       BIGINT default room_id_seq.nextval PRIMARY KEY,
    TITLE    VARCHAR(200)  not null,
    DESC     VARCHAR(200),
    LIVECODE VARCHAR(1000) not null
);

create unique index GEEK.ROOM_ID_UINDEX
    on GEEK.ROOM (ID);

CREATE SEQUENCE room_user_id_seq START WITH 1000;
create table GEEK.ROOM_USER
(
    ID        BIGINT DEFAULT room_user_id_seq.nextval PRIMARY KEY,
    ROOMID    BIGINT       not null,
    USERID    BIGINT       not null,
    MEMBERIDS VARCHAR(200) not null,
    constraint ROOM_USER_ROOM_ID_FK
        foreign key (ROOMID) references GEEK.ROOM (ID)
            on update cascade on delete cascade,
    constraint ROOM_USER_USER_ID_FK
        foreign key (USERID) references GEEK.USER (ID)
            on update cascade on delete cascade
);

create unique index GEEK.ROOM_USER_ID_UINDEX
    on GEEK.ROOM_USER (ID);


-- room 房间信息