-- 创建数据库
CREATE DATABASE IF NOT EXISTS football_platform DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE football_platform;

-- 1. 用户核心表 (统一登录入口)
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名/账号',
  `nickname` varchar(64) NOT NULL UNIQUE COMMENT '用户昵称（唯一）',
  `password` varchar(255) NOT NULL COMMENT '密码(加密)',
  `role` varchar(20) NOT NULL COMMENT '角色: FAN, PLAYER, CLUB, ADMIN',
  `avatar` varchar(255) DEFAULT '/images/default-avatar.png' COMMENT '头像路径',
  `star_level` tinyint DEFAULT 0 COMMENT '球迷评分星级（1-5）',
  `phone` varchar(20) DEFAULT NULL,
  `status` tinyint(2) DEFAULT 1 COMMENT '1:正常 0:禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_nickname` (`nickname`)
) ENGINE=InnoDB COMMENT='系统用户表';

-- 2. 球员详细信息表
CREATE TABLE `player_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '关联sys_user.id',
  `real_name` varchar(64) DEFAULT NULL,
  `height` int(11) DEFAULT NULL COMMENT 'cm',
  `weight` int(11) DEFAULT NULL COMMENT 'kg',
  `position` varchar(32) DEFAULT NULL COMMENT '位置: 前锋/中场/后卫/门将',
  `team_id` bigint(20) DEFAULT NULL COMMENT '所属球队ID',
  `goals` int(11) DEFAULT 0,
  `assists` int(11) DEFAULT 0,
  `market_value` decimal(10,2) DEFAULT 0 COMMENT '身价（万元）',
  `is_free_agent` tinyint DEFAULT 1 COMMENT '是否自由身 1=是 0=否',
  `transfer_record` text COMMENT '转会记录',
  `match_record` text COMMENT '比赛记录',
  `review_count` int DEFAULT 0 COMMENT '球迷评价数',
  `review_score` decimal(2,1) DEFAULT 0 COMMENT '球迷评分（1-5）',
  `status` varchar(20) DEFAULT '自由身' COMMENT '状态：自由身、俱乐部成员',
  `join_status` varchar(20) DEFAULT NULL COMMENT '加入状态：待审核、已审核、拒绝',
  `apply_team_id` bigint(20) DEFAULT NULL COMMENT '申请加入的俱乐部ID',
  `apply_reason` text COMMENT '申请理由',
  `admin_remark` text COMMENT '管理员审核备注',
  `club_remark` text COMMENT '俱乐部审核备注',
  PRIMARY KEY (`id`),
  KEY `idx_player_club` (`team_id`),
  KEY `idx_player_user` (`user_id`),
  KEY `idx_player_apply_team` (`apply_team_id`),
  CONSTRAINT `fk_player_info_team` FOREIGN KEY (`team_id`) REFERENCES `team_club` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_player_info_apply_team` FOREIGN KEY (`apply_team_id`) REFERENCES `team_club` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='球员档案表';

-- 3. 俱乐部/球队表
CREATE TABLE `team_club` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `logo` varchar(255) DEFAULT '/images/default-logo.png',
  `manager_id` bigint(20) DEFAULT NULL COMMENT '关联sys_user.id (俱乐部管理员)',
  `description` text,
  `head_coach` varchar(64) COMMENT '主教练',
  `translator` varchar(64) COMMENT '翻译',
  `sponsor` varchar(128) COMMENT '赞助商',
  `create_by_admin` bigint(20) NOT NULL COMMENT '创建管理员ID',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_team_club_manager` FOREIGN KEY (`manager_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='俱乐部/球队表';

-- 3.1 教练/工作人员表（主教练/翻译/助理教练）
CREATE TABLE `coach` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `club_id` bigint(20) NOT NULL COMMENT '关联 team_club.id',
  `name` varchar(64) NOT NULL COMMENT '姓名',
  `position` varchar(32) NOT NULL COMMENT '职位：主教练/翻译/助理教练',
  `age` int DEFAULT NULL,
  `nationality` varchar(32) DEFAULT NULL,
  `contract_end_date` varchar(32) DEFAULT NULL,
  `salary` decimal(12,2) DEFAULT 0.00 COMMENT '薪资/价值',
  PRIMARY KEY (`id`),
  KEY `idx_coach_club` (`club_id`),
  KEY `idx_coach_position` (`position`),
  CONSTRAINT `fk_coach_club` FOREIGN KEY (`club_id`) REFERENCES `team_club` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='俱乐部工作人员表';

-- 4. 赛事表
CREATE TABLE `football_match` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `cover` varchar(255) DEFAULT '/images/default-match.png',
  `league_id` bigint(20) DEFAULT NULL COMMENT '所属联赛ID',
  `home_team_id` bigint(20) DEFAULT NULL COMMENT '主队俱乐部ID',
  `away_team_id` bigint(20) DEFAULT NULL COMMENT '客队俱乐部ID',
  `location` varchar(255) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `status` varchar(20) DEFAULT 'REGISTERING' COMMENT '状态: 报名中, 进行中, 已结束',
  `home_score` int DEFAULT NULL COMMENT '主队比分',
  `away_score` int DEFAULT NULL COMMENT '客队比分',
  `view_count` bigint(20) DEFAULT 0 COMMENT '热度/浏览量',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者ID(管理员)',
  PRIMARY KEY (`id`),
  KEY `idx_match_league` (`league_id`),
  KEY `idx_match_status` (`status`),
  KEY `idx_match_start_time` (`start_time`),
  CONSTRAINT `fk_match_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_match_home_club` FOREIGN KEY (`home_team_id`) REFERENCES `team_club` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_match_away_club` FOREIGN KEY (`away_team_id`) REFERENCES `team_club` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='赛事表';

-- 5. 互动评论表
CREATE TABLE `fan_comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `match_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `content` text NOT NULL,
  `likes` int(11) DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_match` (`match_id`),
  KEY `idx_comment_user` (`user_id`)
) ENGINE=InnoDB COMMENT='评论表';

-- 6.1 评论回复表（支持回复回复）
CREATE TABLE `fan_comment_reply` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `comment_id` bigint(20) NOT NULL COMMENT '顶级评论ID(对应 fan_comment.id)',
  `parent_reply_id` bigint(20) DEFAULT NULL COMMENT '被回复的回复ID(顶级回复为NULL)',
  `reply_to_user_id` bigint(20) DEFAULT NULL COMMENT '被回复的用户ID（用于@提醒）',
  `reply_user_id` bigint(20) NOT NULL COMMENT '回复者用户ID',
  `content` text NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reply_comment` (`comment_id`),
  KEY `idx_reply_parent` (`parent_reply_id`),
  KEY `idx_reply_user` (`reply_user_id`),
  CONSTRAINT `fk_reply_comment` FOREIGN KEY (`comment_id`) REFERENCES `fan_comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='评论回复表';

-- 6. 赛事报名表（俱乐部报名）
CREATE TABLE `match_registration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `match_id` bigint(20) NOT NULL,
  `team_club_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL COMMENT '报名俱乐部管理员user_id',
  `status` varchar(20) DEFAULT 'REGISTERED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_club` (`match_id`, `team_club_id`),
  CONSTRAINT `fk_match_registration_club` FOREIGN KEY (`team_club_id`) REFERENCES `team_club` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='赛事报名表';

-- 7. 俱乐部关注关系（球迷关注俱乐部）
CREATE TABLE `fan_follow_club` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fan_user_id` bigint(20) NOT NULL,
  `team_club_id` bigint(20) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fan_club` (`fan_user_id`, `team_club_id`),
  CONSTRAINT `fk_fan_follow_club_fan` FOREIGN KEY (`fan_user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fan_follow_club_club` FOREIGN KEY (`team_club_id`) REFERENCES `team_club` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='球迷关注俱乐部表';

-- 8. 平台联系方式（用于“关注我们/联系我们”展示）
CREATE TABLE `platform_contact` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `official_wechat` varchar(64) DEFAULT NULL,
  `official_email` varchar(128) DEFAULT NULL,
  `official_phone` varchar(32) DEFAULT NULL,
  `official_qq` varchar(32) DEFAULT NULL,
  `official_website` varchar(128) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='平台联系方式表';

-- 9. 联赛表
CREATE TABLE `league` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '联赛名称',
  `cover` varchar(255) DEFAULT '/images/league/default.png' COMMENT '联赛封面',
  `description` text COMMENT '联赛描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_league_name` (`name`)
) ENGINE=InnoDB COMMENT='联赛表';

-- 10. 俱乐部-联赛关联表
CREATE TABLE `club_league_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `club_id` bigint(20) NOT NULL COMMENT '俱乐部ID',
  `league_id` bigint(20) NOT NULL COMMENT '联赛ID',
  `join_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_club_league` (`club_id`, `league_id`),
  KEY `idx_club_league` (`club_id`),
  CONSTRAINT `fk_club_league_club` FOREIGN KEY (`club_id`) REFERENCES `team_club` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_club_league_league` FOREIGN KEY (`league_id`) REFERENCES `league` (`id`)
) ENGINE=InnoDB COMMENT='俱乐部-联赛关联表';

-- 11. 球员-俱乐部申请关联表
CREATE TABLE `player_club_apply` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `player_id` bigint(20) NOT NULL COMMENT '球员ID',
  `club_id` bigint(20) NOT NULL COMMENT '俱乐部ID',
  `apply_status` tinyint(1) DEFAULT 0 COMMENT '0=待审核 1=通过 2=拒绝',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `audit_time` datetime COMMENT '审核时间',
  `audit_by` bigint(20) COMMENT '审核管理员ID',
  PRIMARY KEY (`id`),
  KEY `idx_apply_status` (`apply_status`),
  KEY `idx_apply_player` (`player_id`),
  KEY `idx_apply_club` (`club_id`),
  CONSTRAINT `fk_player_club_apply_player` FOREIGN KEY (`player_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_player_club_apply_club` FOREIGN KEY (`club_id`) REFERENCES `team_club` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='球员-俱乐部申请关联表';

-- 12. 球迷-主队关联表
CREATE TABLE `fan_team_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fan_id` bigint(20) NOT NULL COMMENT '球迷ID',
  `club_id` bigint(20) NOT NULL COMMENT '主队ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fan_club` (`fan_id`, `club_id`),
  KEY `idx_fan_team` (`fan_id`),
  CONSTRAINT `fk_fan_team_fan` FOREIGN KEY (`fan_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_fan_team_club` FOREIGN KEY (`club_id`) REFERENCES `team_club` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='球迷-主队关联表';

-- 13. 赛事评分表
CREATE TABLE `match_score` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fan_id` bigint(20) NOT NULL COMMENT '球迷ID',
  `match_id` bigint(20) NOT NULL COMMENT '赛事ID',
  `star_score` tinyint(1) NOT NULL COMMENT '评分星级（1-5）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fan_match` (`fan_id`, `match_id`),
  KEY `idx_match_score` (`match_id`),
  CONSTRAINT `fk_match_score_fan` FOREIGN KEY (`fan_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_match_score_match` FOREIGN KEY (`match_id`) REFERENCES `football_match` (`id`)
) ENGINE=InnoDB COMMENT='赛事评分表';

-- 初始化联系方式数据（可按需修改）
INSERT INTO `platform_contact` (`official_wechat`,`official_email`,`official_phone`,`official_qq`,`official_website`,`remark`)
VALUES ('绿茵链知-公众号', 'support@football-platform.com', '13800138000', '123456789', 'https://football-platform.com', '欢迎关注绿茵链知，获取赛事资讯与球员动态。');

-- 插入测试数据
INSERT INTO `sys_user` (`username`, `nickname`, `password`, `role`, `phone`) VALUES
('admin', '管理员', '$2a$10$E54sX5p20z1y1jH7Qkq2g.lrY2FzZ7eG7eG7eG7eG7eG7eG7eG7e', 'ADMIN', '13800138000'),
('player1', '毛锦号', '$2a$10$E54sX5p20z1y1jH7Qkq2g.lrY2FzZ7eG7eG7eG7eG7eG7eG7eG7e', 'PLAYER', '13800138001'),
('fan1', '球迷张三', '$2a$10$E54sX5p20z1y1jH7Qkq2g.lrY2FzZ7eG7eG7eG7eG7eG7eG7eG7e', 'FAN', '13800138002'),
('club1', '俱乐部管理员', '$2a$10$E54sX5p20z1y1jH7Qkq2g.lrY2FzZ7eG7eG7eG7eG7eG7eG7eG7e', 'CLUB', '13800138003');

-- 插入球员信息测试数据
INSERT INTO `player_info` (`user_id`, `real_name`, `height`, `weight`, `position`, `goals`, `assists`, `market_value`, `is_free_agent`, `status`) VALUES
(2, '毛锦号', 176, 70, '前锋', 10, 5, 1000.00, 1, '自由身');

-- 注意：密码均为123456，已使用BCrypt加密