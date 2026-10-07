-- ============================================================
-- 用户逻辑删除（冻结账户）
-- 执行: mysql -u root -p ai_token < migrate_user_logical_delete.sql
-- 可重复执行（幂等）
-- ============================================================

-- 1. 添加 deleted 列（若不存在）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `t_user` ADD COLUMN `deleted` tinyint NOT NULL DEFAULT 0 COMMENT ''逻辑删除: 0-正常 1-已冻结'' AFTER `group_status`',
    'SELECT ''列 deleted 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 添加索引（若不存在）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND INDEX_NAME = 'idx_deleted'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX `idx_deleted` ON `t_user`(`deleted`)',
    'SELECT ''索引 idx_deleted 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 验证
SELECT '=== t_user deleted 列 ===' AS '';
SHOW COLUMNS FROM `t_user` LIKE 'deleted';
SELECT COUNT(*) AS `已冻结账号数` FROM `t_user` WHERE `deleted` = 1;
