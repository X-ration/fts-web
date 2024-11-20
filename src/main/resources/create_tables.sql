-- create database
CREATE DATABASE IF NOT EXISTS `fts_web` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE fts_web;

-- create tables
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`(
    `id` BIGINT(64) NOT NULL AUTO_INCREMENT,
    `fts_id` BIGINT(64) NOT NULL UNIQUE,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `nickname` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `password` VARCHAR(32) NOT NULL,
    `salt` VARCHAR(32) NOT NULL,
    `is_enabled` BOOL NOT NULL DEFAULT TRUE,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
)ENGINE=INNODB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `user_extend`;
CREATE TABLE `user_extend`(
    `id` BIGINT(64) NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT(64) NOT NULL,
    `birth_date` DATE,
    `hobby` VARCHAR(100),
    `autograph` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY(`id`)
)ENGINE=INNODB DEFAULT CHARSET=utf8;

-- 记录最大fts_id，fts_id从10000开始分配
DROP TABLE IF EXISTS `user_fts_id`;
CREATE TABLE `user_fts_id`(
    `max_fts_id` BIGINT(64) NOT NULL DEFAULT 9999
)ENGINE=INNODB DEFAULT CHARSET=utf8;
INSERT INTO `user_fts_id`(`max_fts_id`) VALUES (9999);