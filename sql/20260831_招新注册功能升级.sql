-- 202608312329 - 新增招新注册表单发布状态。
-- 执行后默认暂停，必须由管理员、主席或秘书长在系统中手动发布。

CREATE TABLE IF NOT EXISTS `sys_recruitment_form` (
  `id` BIGINT NOT NULL COMMENT '固定为1的单例配置ID',
  `is_published` TINYINT NOT NULL DEFAULT 0 COMMENT '是否发布：0暂停，1发布',
  `published_by` BIGINT UNSIGNED NULL COMMENT '最近一次发布人员ID',
  `published_at` DATETIME NULL COMMENT '最近一次发布时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '最近一次状态修改人员ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_recruitment_published_by`
    FOREIGN KEY (`published_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_recruitment_updated_by`
    FOREIGN KEY (`updated_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_recruitment_published` CHECK (`is_published` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='招新注册表单发布状态';

INSERT INTO `sys_recruitment_form`
  (`id`, `is_published`, `published_by`, `published_at`, `updated_by`)
VALUES
  (1, 0, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE `id` = `id`;
