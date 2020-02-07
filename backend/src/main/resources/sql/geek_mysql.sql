# create schema GEEK;


create table `geek`.`room`
(
    ID        BIGINT NOT NULL unique AUTO_INCREMENT primary key,
    TITLE     varchar(200) not null,
    `DESC`      varchar(200),
    LIVECODE  varchar(1000) not null,
    HOSTCODE  varchar(200)                                       not null,
    SERVERURL varchar(200)                                       not null,
    HOSTID    BIGINT                                             not null
)ENGINE = InnoDB,auto_increment=1000;


# 用户信息
create table `geek`.`user`
(
    ID       BIGINT  NOT NULL unique AUTO_INCREMENT primary key,
    `NAME`     VARCHAR(100)                                       not null,
    PASSWORD VARCHAR(300)                                       not null,
    AVATAR   VARCHAR(300)
);


# 录像信息
create table `geek`.`video`
(
    ID        BIGINT  NOT NULL unique AUTO_INCREMENT primary key,
    USERID    BIGINT                                              not null,
    ROOMID    BIGINT                                              not null,
    TIMESTAMP BIGINT                                              not null,
    FILENAME  VARCHAR(300)                                        not null,
    LENGTH    VARCHAR(100)                                        not null
);
