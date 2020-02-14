-- geek 后台表

-- user 用户信息
create schema GEEK;


-- room 房间信息
CREATE SEQUENCE room_id_seq START WITH 1000;
create table GEEK.ROOM
(
    ID        BIGINT default NEXT VALUE FOR room_id_seq not null
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


-- room 用户信息
CREATE SEQUENCE user_id_seq START WITH 100000;
create table GEEK.USER
(
    ID       BIGINT default NEXT VALUE FOR user_id_seq not null
        primary key,
    NAME     VARCHAR(100)                                       not null,
    PASSWORD VARCHAR(300)                                       not null,
    AVATAR   VARCHAR(300),
    GENDER   INT,
    AGE   INT,
    ADDRESS   VARCHAR(100),
);

create unique index GEEK.USER_ID_UINDEX
    on GEEK.USER (ID);

-- room 录像信息
CREATE SEQUENCE video_id_seq START WITH 10000;
create table GEEK.VIDEO
(
    ID        BIGINT default NEXT VALUE FOR video_id_seq not null,
    USERID    BIGINT                                              not null,
    ROOMID    BIGINT                                              not null,
    TIMESTAMP BIGINT                                              not null,
    FILENAME  VARCHAR(300)                                        not null,
    LENGTH    VARCHAR(100)                                        not null,
    constraint VIDEO_PK
        primary key (ID)
);

create unique index GEEK.VIDEO_ID_UINDEX
    on GEEK.VIDEO (ID);


