-- geek 后台表

create schema GEEK;

-- user 用户信息
CREATE SEQUENCE user_id_seq START WITH 1000;
create table GEEK.USER
(
    ID       BIGINT auto_increment PRIMARY KEY,
    NAME     VARCHAR(100) not null,
    PASSWORD VARCHAR(300) not null,
    AVATAR   VARCHAR(300)
);

create unique index GEEK.USER_ID_UINDEX
    on GEEK.USER (ID);

-- 房间信息
CREATE SEQUENCE room_id_seq START WITH 1000;
create table GEEK.ROOM
(
    ID       BIGINT default room_id_seq.nextval PRIMARY KEY,
    TITLE    VARCHAR(200)  not null,
    DESC     VARCHAR(200),
    LIVECODE VARCHAR(1000) not null,--4个成员 livecode
    USERID    BIGINT       not null,--房主的用户id
    MEMBERIDS VARCHAR(200) not null, --3个成员的id
    constraint ROOM_USER_USER_ID_FK
        foreign key (USERID) references GEEK.USER (ID)
            on update cascade on delete cascade
);

create unique index GEEK.ROOM_ID_UINDEX
    on GEEK.ROOM (ID);
