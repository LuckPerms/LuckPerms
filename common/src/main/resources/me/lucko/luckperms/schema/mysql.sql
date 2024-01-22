-- LuckPerms MySQL Schema

CREATE TABLE `luckperms_user_permissions` (
  `id`         INT AUTO_INCREMENT NOT NULL,
  `uuid`       VARCHAR(36)        NOT NULL,
  `permission` VARCHAR(200)       NOT NULL,
  `value`      BOOLEAN            NOT NULL,
  `server`     VARCHAR(36)        NOT NULL,
  `world`      VARCHAR(64)        NOT NULL,
  `expiry`     BIGINT             NOT NULL,
  `contexts`   VARCHAR(200)       NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8mb4;
CREATE INDEX `luckperms_user_permissions_uuid` ON `luckperms_user_permissions` (`uuid`);

CREATE TABLE `luckperms_group_permissions` (
  `id`         INT AUTO_INCREMENT NOT NULL,
  `name`       VARCHAR(36)        NOT NULL,
  `permission` VARCHAR(200)       NOT NULL,
  `value`      BOOLEAN            NOT NULL,
  `server`     VARCHAR(36)        NOT NULL,
  `world`      VARCHAR(64)        NOT NULL,
  `expiry`     BIGINT             NOT NULL,
  `contexts`   VARCHAR(200)       NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8mb4;
CREATE INDEX `luckperms_group_permissions_name` ON `luckperms_group_permissions` (`name`);

CREATE TABLE `{prefix}players` (
  `uuid`          VARCHAR(36) NOT NULL,
  `username`      VARCHAR(16) NOT NULL,
  `primary_group` VARCHAR(36) NOT NULL,
  PRIMARY KEY (`uuid`)
) DEFAULT CHARSET = utf8mb4;
CREATE INDEX `luckperms_players_username` ON `luckperms_players` (`username`);

CREATE TABLE `{prefix}groups` (
  `name` VARCHAR(36) NOT NULL,
  PRIMARY KEY (`name`)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE `luckperms_actions` (
  `id`         INT AUTO_INCREMENT NOT NULL,
  `time`       BIGINT             NOT NULL,
  `actor_uuid` VARCHAR(36)        NOT NULL,
  `actor_name` VARCHAR(100)       NOT NULL,
  `type`       CHAR(1)            NOT NULL,
  `acted_uuid` VARCHAR(36)        NOT NULL,
  `acted_name` VARCHAR(36)        NOT NULL,
  `action`     VARCHAR(300)       NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE `luckperms_tracks` (
  `name`   VARCHAR(36) NOT NULL,
  `groups` TEXT        NOT NULL,
  PRIMARY KEY (`name`)
) DEFAULT CHARSET = utf8mb4;
