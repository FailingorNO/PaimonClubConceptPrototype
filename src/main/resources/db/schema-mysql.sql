
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
 SET NAMES utf8mb4 ;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `t_audit_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cfg_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `cfg_value` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKidhj09jppyt7p0wfpwuktmqh8` (`cfg_key`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_decoration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_decoration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `price` int(11) NOT NULL,
  `resourceUrl` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` int(11) NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_favorite` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `postId` bigint(20) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_follow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `followeeId` bigint(20) NOT NULL,
  `followerId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_friend` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `friendId` bigint(20) NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_gacha_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
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
) ENGINE=InnoDB AUTO_INCREMENT=361 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_game_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_game_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_game_score` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `gameId` bigint(20) NOT NULL,
  `score` int(11) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_like` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `targetId` bigint(20) NOT NULL,
  `targetType` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `fromUser` bigint(20) NOT NULL,
  `isRead` bit(1) NOT NULL,
  `toUser` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_mini_game`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_mini_game` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `icon` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `path` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `rewardPoints` int(11) NOT NULL,
  `status` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `isRead` bit(1) NOT NULL,
  `refId` bigint(20) DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `pointsCost` int(11) NOT NULL,
  `productId` bigint(20) NOT NULL,
  `productName` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_points_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_points_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` int(11) NOT NULL,
  `createdAt` datetime(6) NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `refId` bigint(20) DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_post_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_post_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `icon` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `sort` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_post_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_post_image` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `postId` bigint(20) NOT NULL,
  `sort` int(11) NOT NULL,
  `url` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_share`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_share` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `postId` bigint(20) NOT NULL,
  `shareCode` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9k0j2keiygkk59nvcoa0cpxow` (`shareCode`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_user_decoration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `t_user_decoration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) NOT NULL,
  `decorationId` bigint(20) NOT NULL,
  `expireAt` datetime(6) DEFAULT NULL,
  `isWearing` bit(1) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

