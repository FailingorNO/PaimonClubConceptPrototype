-- 清理浏览器自动化验证产生的临时账号及其数据（仅 browtest01）
SET NAMES utf8mb4;

DELETE FROM t_game_character WHERE userId IN (SELECT id FROM t_user WHERE username = 'browtest01');
DELETE FROM t_game_account   WHERE userId IN (SELECT id FROM t_user WHERE username = 'browtest01');
DELETE FROM t_comment        WHERE userId IN (SELECT id FROM t_user WHERE username = 'browtest01');
DELETE FROM t_like           WHERE userId IN (SELECT id FROM t_user WHERE username = 'browtest01');
DELETE FROM t_favorite       WHERE userId IN (SELECT id FROM t_user WHERE username = 'browtest01');
DELETE FROM t_points_record  WHERE userId IN (SELECT id FROM t_user WHERE username = 'browtest01');
DELETE FROM t_post           WHERE userId IN (SELECT id FROM t_user WHERE username = 'browtest01');
DELETE FROM t_user           WHERE username = 'browtest01';

SELECT COUNT(*) AS user_count FROM t_user;
SELECT COUNT(*) AS post_count FROM t_post;
SELECT COUNT(*) AS character_count FROM t_game_character;
