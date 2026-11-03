-- 清理端到端自动化测试产生的账号与内容（仅 e2e 前缀账号）
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `t_game_character` WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%');
DELETE FROM `t_game_account`   WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%');
DELETE FROM `t_gacha_record`   WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%');
DELETE FROM `t_points_record`  WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%');
DELETE FROM `t_like`           WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%');
DELETE FROM `t_favorite`       WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%');
DELETE FROM `t_comment`        WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%');
DELETE FROM `t_post_image`     WHERE `postId` IN (SELECT `id` FROM `t_post` WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%'));
DELETE FROM `t_post`           WHERE `userId` IN (SELECT `id` FROM `t_user` WHERE `username` LIKE 'e2e%');
DELETE FROM `t_user`           WHERE `username` LIKE 'e2e%';

SET FOREIGN_KEY_CHECKS = 1;

SELECT COUNT(*) AS user_count FROM `t_user`;
SELECT COUNT(*) AS post_count FROM `t_post`;
SELECT COUNT(*) AS character_count FROM `t_game_character`;
