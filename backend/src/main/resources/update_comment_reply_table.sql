-- 用于把 existing 数据库升级到支持评论回复：fan_comment_reply
USE football_platform;

SET @tbl_exists := (
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = 'football_platform'
    AND TABLE_NAME = 'fan_comment_reply'
);

SET @sql := IF(@tbl_exists = 0,
  'CREATE TABLE `fan_comment_reply` (
     `id` bigint(20) NOT NULL AUTO_INCREMENT,
     `comment_id` bigint(20) NOT NULL COMMENT ''顶级评论ID(对应 fan_comment.id)'',
     `parent_reply_id` bigint(20) DEFAULT NULL COMMENT ''被回复的回复ID(顶级回复为NULL)'',
     `reply_to_user_id` bigint(20) DEFAULT NULL COMMENT ''被回复的用户ID（用于@提醒）'',
     `reply_user_id` bigint(20) NOT NULL COMMENT ''回复者用户ID'',
     `content` text NOT NULL,
     `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`),
     KEY `idx_reply_comment` (`comment_id`),
     KEY `idx_reply_parent` (`parent_reply_id`),
     KEY `idx_reply_user` (`reply_user_id`),
     CONSTRAINT `fk_reply_comment` FOREIGN KEY (`comment_id`) REFERENCES `fan_comment` (`id`) ON DELETE CASCADE
   ) ENGINE=InnoDB COMMENT=''评论回复表'';',
  'SELECT ''fan_comment_reply already exists'';'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

