-- 用于将 existing 数据库升级到支持主队/客队选择的结构
-- 注意：如果你已经手动执行过 ALTER TABLE，可跳过本脚本

-- 添加 home_team_id
SET @cnt_home := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'football_match'
    AND COLUMN_NAME = 'home_team_id'
);
SET @sql_home := IF(@cnt_home = 0,
  'ALTER TABLE football_match ADD COLUMN home_team_id BIGINT NULL;',
  'SELECT 1;'
);
PREPARE stmt_home FROM @sql_home;
EXECUTE stmt_home;
DEALLOCATE PREPARE stmt_home;

-- 添加 away_team_id
SET @cnt_away := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'football_match'
    AND COLUMN_NAME = 'away_team_id'
);
SET @sql_away := IF(@cnt_away = 0,
  'ALTER TABLE football_match ADD COLUMN away_team_id BIGINT NULL;',
  'SELECT 1;'
);
PREPARE stmt_away FROM @sql_away;
EXECUTE stmt_away;
DEALLOCATE PREPARE stmt_away;

-- 添加 league_id
SET @cnt_league := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'football_match'
    AND COLUMN_NAME = 'league_id'
);
SET @sql_league := IF(@cnt_league = 0,
  'ALTER TABLE football_match ADD COLUMN league_id BIGINT NULL;',
  'SELECT 1;'
);
PREPARE stmt_league FROM @sql_league;
EXECUTE stmt_league;
DEALLOCATE PREPARE stmt_league;

-- 添加 home_score
SET @cnt_home_score := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'football_match'
    AND COLUMN_NAME = 'home_score'
);
SET @sql_home_score := IF(@cnt_home_score = 0,
  'ALTER TABLE football_match ADD COLUMN home_score INT NULL;',
  'SELECT 1;'
);
PREPARE stmt_home_score FROM @sql_home_score;
EXECUTE stmt_home_score;
DEALLOCATE PREPARE stmt_home_score;

-- 添加 away_score
SET @cnt_away_score := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'football_match'
    AND COLUMN_NAME = 'away_score'
);
SET @sql_away_score := IF(@cnt_away_score = 0,
  'ALTER TABLE football_match ADD COLUMN away_score INT NULL;',
  'SELECT 1;'
);
PREPARE stmt_away_score FROM @sql_away_score;
EXECUTE stmt_away_score;
DEALLOCATE PREPARE stmt_away_score;

