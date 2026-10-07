-- 公告管理表
CREATE TABLE `t_announcement` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`        varchar(200) NOT NULL                COMMENT '公告标题',
  `content`      text         NOT NULL                COMMENT '公告内容（支持 HTML）',
  `publisher`    varchar(64)  NOT NULL                COMMENT '发布人',
  `show_on_home` tinyint(1)   NOT NULL DEFAULT 0      COMMENT '是否显示在主页面: 0-否 1-是（全局唯一）',
  `created_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_show_on_home` (`show_on_home`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统公告';
