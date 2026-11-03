-- ===============================================================
-- 派蒙社（Paimon Club）一键建库脚本
-- 数据库：paimonclub    字符集：utf8mb4    引擎：InnoDB
-- 内容：建库 + 25 张表 + 系统基础数据
--       基础数据只有：唯一管理员账号 + 板块/商品/装扮/小游戏/配置等字典
--       帖子、评论、角色、抽卡记录等一切用户内容都不预置，由用户自行产生
-- 用法：mysql -u root -p --default-character-set=utf8mb4 < sql/init-paimonclub.sql
-- 说明：脚本可重复执行（先 DROP 再建表），会清空同名库中的旧数据
-- ===============================================================

CREATE DATABASE IF NOT EXISTS `paimonclub` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `paimonclub`;

-- ---------------------------------------------------------------
-- 1. 建库参数
-- ---------------------------------------------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------
-- 2. 表结构（25 张）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `t_audit_record`;
CREATE TABLE `t_audit_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `auditorId` bigint(20) DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `result` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `targetId` bigint(20) NOT NULL,
  `targetType` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `likeCount` int(11) NOT NULL,
  `parentId` bigint(20) DEFAULT NULL,
  `postId` bigint(20) NOT NULL,
  `rootId` bigint(20) DEFAULT NULL,
  `status` int(11) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_config`;
CREATE TABLE `t_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cfg_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `cfg_value` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKidhj09jppyt7p0wfpwuktmqh8` (`cfg_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_decoration`;
CREATE TABLE `t_decoration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `price` int(11) NOT NULL,
  `resourceUrl` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` int(11) NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_favorite`;
CREATE TABLE `t_favorite` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `postId` bigint(20) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_follow`;
CREATE TABLE `t_follow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `followeeId` bigint(20) NOT NULL,
  `followerId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_friend`;
CREATE TABLE `t_friend` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `friendId` bigint(20) NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_gacha_record`;
CREATE TABLE `t_gacha_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `gameUid` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `itemName` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `itemType` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `poolName` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `pullTime` datetime(6) NOT NULL,
  `rarity` int(11) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_game_account`;
CREATE TABLE `t_game_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `adventure_rank` int(11) DEFAULT NULL,
  `bindTime` datetime(6) NOT NULL,
  `gameName` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `gameUid` varchar(30) COLLATE utf8mb4_general_ci NOT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `server` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `userId` bigint(20) NOT NULL,
  `world_level` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_game_character`;
CREATE TABLE `t_game_character` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `char_level` int(11) DEFAULT NULL,
  `constellation` int(11) DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `element` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  `weapon` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_game_score`;
CREATE TABLE `t_game_score` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `gameId` bigint(20) NOT NULL,
  `score` int(11) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_like`;
CREATE TABLE `t_like` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `targetId` bigint(20) NOT NULL,
  `targetType` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `fromUser` bigint(20) NOT NULL,
  `isRead` bit(1) NOT NULL,
  `toUser` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_mini_game`;
CREATE TABLE `t_mini_game` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `icon` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `path` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `rewardPoints` int(11) NOT NULL,
  `status` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_notification`;
CREATE TABLE `t_notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `isRead` bit(1) NOT NULL,
  `refId` bigint(20) DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `pointsCost` int(11) NOT NULL,
  `productId` bigint(20) NOT NULL,
  `productName` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_points_record`;
CREATE TABLE `t_points_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` int(11) NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `refId` bigint(20) DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_post`;
CREATE TABLE `t_post` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `categoryId` bigint(20) NOT NULL,
  `commentCount` int(11) NOT NULL,
  `content` varchar(5000) COLLATE utf8mb4_general_ci NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `favoriteCount` int(11) NOT NULL,
  `likeCount` int(11) NOT NULL,
  `rejectReason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `tags` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `is_top` bit(1) NOT NULL,
  `updatedAt` datetime(6) NOT NULL,
  `userId` bigint(20) NOT NULL,
  `viewCount` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_post_category`;
CREATE TABLE `t_post_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `icon` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `sort` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_post_image`;
CREATE TABLE `t_post_image` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `postId` bigint(20) NOT NULL,
  `sort` int(11) NOT NULL,
  `url` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `image` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `pricePoints` int(11) NOT NULL,
  `status` int(11) NOT NULL,
  `stock` int(11) NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_report`;
CREATE TABLE `t_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `handleResult` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `handlerId` bigint(20) DEFAULT NULL,
  `reason` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `reporterId` bigint(20) NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `targetId` bigint(20) NOT NULL,
  `targetType` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_share`;
CREATE TABLE `t_share` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `postId` bigint(20) NOT NULL,
  `shareCode` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9k0j2keiygkk59nvcoa0cpxow` (`shareCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `avatarUrl` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `bio` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `createdAt` datetime(6) NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `last_sign_in_date` date DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `passwordHash` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `phone` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `points` int(11) NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `status` int(11) NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjhib4legehrm4yscx9t3lirqi` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TABLE IF EXISTS `t_user_decoration`;
CREATE TABLE `t_user_decoration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `decorationId` bigint(20) NOT NULL,
  `expireAt` datetime(6) DEFAULT NULL,
  `isWearing` bit(1) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------
-- 3. 系统基础数据
--    只有唯一管理员账号 + 功能必需的字典数据；不含任何示例帖子、评论、角色、抽卡记录
-- ---------------------------------------------------------------
INSERT INTO `t_user` (`id`,`username`,`passwordHash`,`nickname`,`avatarUrl`,`role`,`status`,`points`,`bio`,`createdAt`) VALUES
 (1,'admin','$2a$10$e5ozldnr4vub9yDoY2DwPewc9KdsGrCl9Syz3cz86aN4eiP1fs16u','系统管理员','static/img/avatar-default.svg','ADMIN',1,0,'',NOW());

INSERT INTO `t_post_category` (`id`,`name`,`sort`,`description`,`icon`) VALUES
 (1,'攻略分享',1,'深渊、探索、材料路线等硬核攻略','book'),
 (2,'同人创作',2,'绘画、小说、cosplay 作品','palette'),
 (3,'抽卡交流',3,'晒卡、玄学、保底记录','star'),
 (4,'交友组队',4,'找队友、找同好、联机开黑','users'),
 (5,'闲聊灌水',5,'旅行者日常闲聊','coffee');

INSERT INTO `t_product` (`id`,`name`,`description`,`pricePoints`,`stock`,`type`,`status`,`createdAt`) VALUES
 (1,'派蒙金属徽章','限定版派蒙金属徽章，直径 4cm，附赠亚克力立牌。',300,20,'PHYSICAL',1,NOW()),
 (2,'深渊挑战纪念挂件','通关 12 层纪念挂件，限量发售。',500,10,'PHYSICAL',1,NOW()),
 (3,'月卡体验券（7 天）','虚拟道具，兑换后 7 天内每日领取原石。',800,50,'VIRTUAL',1,NOW()),
 (4,'社区专属头衔「资深旅行者」','虚拟头衔，佩戴后展示在昵称旁。',200,100,'VIRTUAL',1,NOW());

INSERT INTO `t_decoration` (`id`,`name`,`type`,`resourceUrl`,`price`,`status`) VALUES
 (1,'金色派蒙头像框','AVATAR_FRAME','static/img/frame-gold.svg',300,1),
 (2,'深渊星辉头像框','AVATAR_FRAME','static/img/frame-abyss.svg',500,1),
 (3,'头衔·欧皇','TITLE',NULL,400,1),
 (4,'头衔·资深旅行者','TITLE',NULL,200,1);

INSERT INTO `t_mini_game` (`id`,`name`,`path`,`rewardPoints`,`status`,`description`,`icon`) VALUES
 (1,'贪吃蛇·元素版','games.html',5,1,'方向键控制派蒙吃元素球，撞墙即结束','static/img/game.svg'),
 (2,'2048·原石合成','games.html',5,1,'合成到 2048 获得原石，冲击更高分','static/img/game.svg'),
 (3,'猜数字·命运的骰子','games.html',3,1,'7 次机会猜中 1-100 的数字','static/img/game.svg'),
 (4,'抽卡模拟器','games.html',2,1,'模拟十连，看看你的欧气值','static/img/game.svg');

INSERT INTO `t_config` (`id`,`cfg_key`,`cfg_value`,`remark`) VALUES
 (1,'audit.mode','POST','审核模式：PRE=先审后发，POST=先发后审'),
 (2,'sensitive.enabled','true','是否开启敏感词过滤');

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------
-- 4. 完成：共 25 张表
-- ---------------------------------------------------------------
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'paimonclub';
SELECT COUNT(*) AS user_count FROM `t_user`;
SELECT COUNT(*) AS post_count FROM `t_post`;
