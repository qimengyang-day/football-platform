-- 为已有数据库补齐：俱乐部(team_club)外键约束 + 删除联动
-- 目标：
-- 1) team_club 删除时，club_league_relation / player_club_apply / fan_team_relation / match_registration / fan_follow_club 自动级联删除
-- 2) team_club 删除时，player_info.team_id/applay_team_id 置空，并同步球员状态为“自由身”
-- 3) team_club.manager_id 可空并外检到 sys_user(id)，sys_user 主队 main_team_id 外检到 team_club(id)

USE football_platform;

-- 0) 确保 team_club 主键存在（通常已经存在）
SET @pk_exists = 0;
SELECT COUNT(*) INTO @pk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'team_club'
  AND CONSTRAINT_TYPE = 'PRIMARY KEY';

SET @sql = IF(@pk_exists = 0, 'ALTER TABLE team_club ADD PRIMARY KEY (id);', 'SELECT 1;');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1) manager_id 改为可空（与代码中 managerId null 的行为一致）
ALTER TABLE team_club MODIFY manager_id bigint(20) DEFAULT NULL;

-- 2) 修改已有“无 ON DELETE 动作”的外键：删除旧约束，重新创建
-- 2.1 club_league_relation -> team_club
SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'club_league_relation'
  AND CONSTRAINT_NAME = 'fk_club_league_club';
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE club_league_relation DROP FOREIGN KEY fk_club_league_club;', 'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE club_league_relation
  ADD CONSTRAINT fk_club_league_club
  FOREIGN KEY (club_id) REFERENCES team_club(id) ON DELETE CASCADE;

-- 2.2 player_club_apply -> team_club
SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'player_club_apply'
  AND CONSTRAINT_NAME = 'fk_player_club_apply_club';
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE player_club_apply DROP FOREIGN KEY fk_player_club_apply_club;', 'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE player_club_apply
  ADD CONSTRAINT fk_player_club_apply_club
  FOREIGN KEY (club_id) REFERENCES team_club(id) ON DELETE CASCADE;

-- 2.3 fan_team_relation -> team_club
SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'fan_team_relation'
  AND CONSTRAINT_NAME = 'fk_fan_team_club';
SET @sql = IF(@fk_exists > 0, 'ALTER TABLE fan_team_relation DROP FOREIGN KEY fk_fan_team_club;', 'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE fan_team_relation
  ADD CONSTRAINT fk_fan_team_club
  FOREIGN KEY (club_id) REFERENCES team_club(id) ON DELETE CASCADE;

-- 3) 为原本缺失外键的表补齐：match_registration / fan_follow_club
-- match_registration.team_club_id -> team_club(id)
SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'match_registration'
  AND CONSTRAINT_NAME = 'fk_match_registration_club';
SET @sql = IF(@fk_exists = 0,
  'ALTER TABLE match_registration
    ADD CONSTRAINT fk_match_registration_club
    FOREIGN KEY (team_club_id) REFERENCES team_club(id) ON DELETE CASCADE;',
  'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fan_follow_club.team_club_id -> team_club(id)
SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'fan_follow_club'
  AND CONSTRAINT_NAME = 'fk_fan_follow_club_club';
SET @sql = IF(@fk_exists = 0,
  'ALTER TABLE fan_follow_club
    ADD CONSTRAINT fk_fan_follow_club_club
    FOREIGN KEY (team_club_id) REFERENCES team_club(id) ON DELETE CASCADE;',
  'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) player_info.team_id / apply_team_id -> team_club(id) ON DELETE SET NULL
SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'player_info'
  AND CONSTRAINT_NAME = 'fk_player_info_team';
SET @sql = IF(@fk_exists = 0,
  'ALTER TABLE player_info
    ADD CONSTRAINT fk_player_info_team
    FOREIGN KEY (team_id) REFERENCES team_club(id) ON DELETE SET NULL;',
  'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'player_info'
  AND CONSTRAINT_NAME = 'fk_player_info_apply_team';
SET @sql = IF(@fk_exists = 0,
  'ALTER TABLE player_info
    ADD CONSTRAINT fk_player_info_apply_team
    FOREIGN KEY (apply_team_id) REFERENCES team_club(id) ON DELETE SET NULL;',
  'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) team_club.manager_id -> sys_user(id) ON DELETE SET NULL
SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'team_club'
  AND CONSTRAINT_NAME = 'fk_team_club_manager';
SET @sql = IF(@fk_exists = 0,
  'ALTER TABLE team_club
    ADD CONSTRAINT fk_team_club_manager
    FOREIGN KEY (manager_id) REFERENCES sys_user(id) ON DELETE SET NULL;',
  'SELECT 1;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) sys_user.main_team_id -> team_club(id) ON DELETE SET NULL（如果 main_team_id 存在）
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'sys_user'
  AND COLUMN_NAME = 'main_team_id';

SET @fk_exists = 0;
SELECT COUNT(*) INTO @fk_exists
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'football_platform'
  AND TABLE_NAME = 'sys_user'
  AND CONSTRAINT_NAME = 'fk_sys_user_main_team';
SET @sql = IF(@col_exists > 0 AND @fk_exists = 0,
  'ALTER TABLE sys_user
    ADD CONSTRAINT fk_sys_user_main_team
    FOREIGN KEY (main_team_id) REFERENCES team_club(id) ON DELETE SET NULL;',
  'SELECT 1;');

PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7) 触发器：删除 team_club 后，把 player_info 同步为“自由身”
DROP TRIGGER IF EXISTS trg_team_club_after_delete;
DELIMITER $$
CREATE TRIGGER trg_team_club_after_delete
AFTER DELETE ON team_club
FOR EACH ROW
BEGIN
  UPDATE player_info
  SET
    team_id = NULL,
    is_free_agent = 1,
    status = '自由身',
    join_status = ''
  WHERE team_id = OLD.id;
END$$
DELIMITER ;

