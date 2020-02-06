-- geek 后台表

-- user 用户信息
create schema GEEK;

-- auto-generated definition
-- CREATE SEQUENCE user_id_seq START WITH 1000;
-- create table GEEK.USER
-- (
--     ID       BIGINT DEFAULT user_id_seq.nextval PRIMARY KEY,
--     NAME     VARCHAR(100) not null,
--     PASSWORD VARCHAR(300) not null,
--     AVATAR   VARCHAR(300)
-- );
--
-- create unique index GEEK.USER_ID_UINDEX
--     on GEEK.USER (ID);
--
-- CREATE SEQUENCE room_id_seq START WITH 1000;
-- create table GEEK.ROOM
-- (
--     ID       BIGINT default room_id_seq.nextval PRIMARY KEY,
--     TITLE    VARCHAR(200)  not null,
--     DESC     VARCHAR(200),
--     LIVECODE VARCHAR(1000) not null
-- );
--
-- create unique index GEEK.ROOM_ID_UINDEX
--     on GEEK.ROOM (ID);
--
-- CREATE SEQUENCE room_user_id_seq START WITH 1000;
-- create table GEEK.ROOM_USER
-- (
--     ID        BIGINT DEFAULT room_user_id_seq.nextval PRIMARY KEY,
--     ROOMID    BIGINT       not null,
--     USERID    BIGINT       not null,
--     MEMBERIDS VARCHAR(200) not null,
--     constraint ROOM_USER_ROOM_ID_FK
--         foreign key (ROOMID) references GEEK.ROOM (ID)
--             on update cascade on delete cascade,
--     constraint ROOM_USER_USER_ID_FK
--         foreign key (USERID) references GEEK.USER (ID)
--             on update cascade on delete cascade
-- );
--
-- create unique index GEEK.ROOM_USER_ID_UINDEX
--     on GEEK.ROOM_USER (ID);


-- room 房间信息

--更新数据库结构
CREATE SEQUENCE room_id_seq START WITH 1000;
create table GEEK.ROOM
(
    ID        BIGINT auto_increment not null
        primary key,
    TITLE     VARCHAR(200)                                       not null,
    DESC      VARCHAR(200),
    LIVECODE  VARCHAR(1000)                                      not null,
    HOSTCODE  VARCHAR(200)                                       not null,
    SERVERURL VARCHAR(200)                                       not null,
    HOSTID    BIGINT                                             not null
);

create unique index GEEK.ROOM_ID_UINDEX
    on GEEK.ROOM (ID);


CREATE SEQUENCE user_id_seq START WITH 100000;
create table GEEK.USER
(
    ID       BIGINT auto_increment not null
        primary key,
    NAME     VARCHAR(100)                                       not null,
    PASSWORD VARCHAR(300)                                       not null,
    AVATAR   VARCHAR(300)
);

create unique index GEEK.USER_ID_UINDEX
    on GEEK.USER (ID);


CREATE SEQUENCE video_id_seq START WITH 10000;
create table GEEK.VIDEO
(
    ID        BIGINT auto_increment not null,
    USERID    BIGINT                                              not null,
    ROOMID    BIGINT                                              not null,
    TIMESTAMP BIGINT                                              not null,
    FILENAME  VARCHAR(300)                                        not null,
    LENGTH    VARCHAR(100)                                        not null,
    INVITATION BIGINT                                             not null,
    COMMENT   VARCHAR(500)                                        not null,
    constraint VIDEO_PK
        primary key (ID)
);

create unique index GEEK.VIDEO_ID_UINDEX
    on GEEK.VIDEO (ID);

CREATE SEQUENCE video_id_seq START WITH 10000;
create table GEEK.VIDEO
(
    ID        BIGINT auto_increment not null,
    USERID    BIGINT                                              not null,
    ROOMID    BIGINT                                              not null,
    TIMESTAMP BIGINT                                              not null,
    FILENAME  VARCHAR(300)                                        not null,
    LENGTH    VARCHAR(100)                                        not null,
    INVITATION BIGINT                                             not null,
    COMMENT   VARCHAR(500)                                        not null,
    constraint VIDEO_PK
        primary key (ID)
);

create unique index GEEK.VIDEO_ID_UINDEX
    on GEEK.VIDEO (ID);


