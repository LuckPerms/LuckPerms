-- LuckPerms H2 Schema.

CREATE TABLE `{prefix}user_permissions` (
  `id`         INT AUTO_INCREMENT NOT NULL,
  `uuid`       VARCHAR(36)        NOT NULL,
  `permission` VARCHAR(200)       NOT NULL,
  `value`      BOOL               NOT NULL,
  `server`     VARCHAR(36)        NOT NULL,
  `world`      VARCHAR(64)        NOT NULL,
  `expiry`     BIGINT             NOT NULL,
  `contexts`   VARCHAR(200)       NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE INDEX ON `{prefix}user_permissions` (`uuid`);

CREATE TABLE `{prefix}group_permissions` (
  `id`         INT AUTO_INCREMENT NOT NULL,
  `name`       VARCHAR(36)        NOT NULL,
  `permission` VARCHAR(200)       NOT NULL,
  `value`      BOOL               NOT NULL,
  `server`     VARCHAR(36)        NOT NULL,
  `world`      VARCHAR(64)        NOT NULL,
  `expiry`     BIGINT             NOT NULL,
  `contexts`   VARCHAR(200)       NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE INDEX ON `{prefix}group_permissions` (`name`);

CREATE TABLE `{prefix}players` (
  `uuid`          VARCHAR(36) NOT NULL,
  `username`      VARCHAR(16) NOT NULL,
  `primary_group` VARCHAR(36) NOT NULL,
  PRIMARY KEY (`uuid`)
);
CREATE INDEX ON `{prefix}players` (`username`);

CREATE TABLE `{prefix}groups` (
  `name` VARCHAR(36) NOT NULL,
  PRIMARY KEY (`name`)
);

CREATE TABLE `{prefix}actions` (
  `id`         INT AUTO_INCREMENT NOT NULL,
  `time`       BIGINT             NOT NULL,
  `actor_uuid` VARCHAR(36)        NOT NULL,
  `actor_name` VARCHAR(100)       NOT NULL,
  `type`       CHAR(1)            NOT NULL,
  `acted_uuid` VARCHAR(36)        NOT NULL,
  `acted_name` VARCHAR(36)        NOT NULL,
  `action`     VARCHAR(300)       NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `{prefix}tracks` (
  `name`   VARCHAR(36) NOT NULL,
  `groups` TEXT        NOT NULL,
  PRIMARY KEY (`name`)
);
