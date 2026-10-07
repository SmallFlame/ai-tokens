-- ============================================================
-- v2.0 角色体系重构 + 认证统一迁移脚本
-- 执行方式: mysql -u root -p ai_token < v2.0_role_migration.sql
-- ============================================================

-- 1. 创建角色表
CREATE TABLE IF NOT EXISTS `t_role` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `name`        varchar(32)  NOT NULL COMMENT '角色显示名',
  `code`        varchar(32)  NOT NULL COMMENT '角色编码(super_admin/advisor/leader/member)',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `created_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 2. 插入预置角色
INSERT INTO `t_role` (`id`, `name`, `code`, `description`) VALUES
  (1, '超级管理员', 'super_admin', '所有功能，不受金额与实名限制'),
  (2, '导师',       'advisor',     '用户管理、小组管理、查看全部数据'),
  (3, '组长',       'leader',      '团队管理、查看本组数据、分配金额'),
  (4, '组员',       'member',      '基础功能、查看自己的数据');

-- 3. t_user 新增字段
ALTER TABLE `t_user`
  ADD COLUMN `role_id`    bigint DEFAULT 4 COMMENT '角色ID，关联 t_role.id',
  ADD COLUMN `student_id` varchar(32)  DEFAULT NULL COMMENT '学号/工号',
  ADD COLUMN `avatar`     varchar(512) DEFAULT NULL COMMENT '头像URL',
  ADD INDEX  `idx_role_id` (`role_id`);

-- 4. 数据迁移：根据旧 role 字段映射到新 role_id
--    旧: 0=超管 1=组长 2=组员 3=管理员(导师)
--    新: 1=super_admin 2=advisor 3=leader 4=member
UPDATE `t_user` SET `role_id` = 1 WHERE `role` = 0;
UPDATE `t_user` SET `role_id` = 2 WHERE `role` = 3;
UPDATE `t_user` SET `role_id` = 3 WHERE `role` = 1;
UPDATE `t_user` SET `role_id` = 4 WHERE `role` = 2;

-- 5. t_group 新增字段
ALTER TABLE `t_group`
  ADD COLUMN `semester`   varchar(64)  DEFAULT NULL COMMENT '学期标识',
  ADD COLUMN `created_by` bigint       DEFAULT NULL COMMENT '创建者用户ID',
  ADD COLUMN `updated_at` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 验证
SELECT '=== t_role 表数据 ===' AS '';  SELECT * FROM t_role;
SELECT '=== t_user role/role_id 对照 (前10条) ===' AS '';  SELECT id, username, role, role_id FROM t_user LIMIT 10;
