-- 202609011549 - 修复 MySQL 不支持 ADD COLUMN IF NOT EXISTS 导致的 1064 错误。
-- 请在执行 20260831_招新注册功能升级.sql 后执行本脚本。

-- 通过 information_schema 判断字段是否存在，使脚本可重复执行并兼容 MySQL 8.x。
SET @has_publish_start_at = (
  SELECT COUNT(*)
  FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE()
    AND `TABLE_NAME` = 'sys_recruitment_form'
    AND `COLUMN_NAME` = 'publish_start_at'
);
SET @add_publish_start_at_sql = IF(
  @has_publish_start_at = 0,
  'ALTER TABLE `sys_recruitment_form` ADD COLUMN `publish_start_at` DATETIME NULL COMMENT ''公开注册开放时间，NULL 表示立即开放'' AFTER `published_at`',
  'SELECT 1'
);
PREPARE add_publish_start_at_stmt FROM @add_publish_start_at_sql;
EXECUTE add_publish_start_at_stmt;
DEALLOCATE PREPARE add_publish_start_at_stmt;

SET @has_publish_end_at = (
  SELECT COUNT(*)
  FROM `information_schema`.`COLUMNS`
  WHERE `TABLE_SCHEMA` = DATABASE()
    AND `TABLE_NAME` = 'sys_recruitment_form'
    AND `COLUMN_NAME` = 'publish_end_at'
);
SET @add_publish_end_at_sql = IF(
  @has_publish_end_at = 0,
  'ALTER TABLE `sys_recruitment_form` ADD COLUMN `publish_end_at` DATETIME NULL COMMENT ''公开注册结束时间，NULL 表示不自动结束'' AFTER `publish_start_at`',
  'SELECT 1'
);
PREPARE add_publish_end_at_stmt FROM @add_publish_end_at_sql;
EXECUTE add_publish_end_at_stmt;
DEALLOCATE PREPARE add_publish_end_at_stmt;

-- 202609011558 - 允许匿名注册失败请求写入审计日志，成功注册仍关联新用户ID。
ALTER TABLE `sys_operation_log`
  MODIFY COLUMN `user_id` BIGINT UNSIGNED NULL COMMENT '操作人ID，匿名请求可为空';

-- 已发布的旧表单保持原有可访问性，并以其最近发布时间作为开放时间。
UPDATE `sys_recruitment_form`
SET `publish_start_at` = COALESCE(`publish_start_at`, `published_at`, `update_time`)
WHERE `is_published` = 1
  AND `publish_start_at` IS NULL;
