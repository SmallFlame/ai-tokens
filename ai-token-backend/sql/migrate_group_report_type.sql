-- ============================================================
-- 小组周报类型标记
-- 执行: mysql -u root -p ai_token < migrate_group_report_type.sql
-- 可重复执行（幂等）
-- ============================================================

-- 添加 report_type 列（若不存在）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_group' AND COLUMN_NAME = 'report_type'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `t_group` ADD COLUMN `report_type` int NOT NULL DEFAULT 1 COMMENT ''周报类型: 1-需交周报(默认) 0-不需交'' AFTER `semester`',
    'SELECT ''列 report_type 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 验证
SELECT '=== t_group report_type 列 ===' AS '';
SHOW COLUMNS FROM `t_group` LIKE 'report_type';
