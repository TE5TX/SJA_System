-- 202609011700 - 完成了人员永久删除前保留历史审计日志所需的数据库升级。
-- 删除人员时，应用会将该人员作为“操作人”的历史日志解除外键引用，日志内容本身不会删除。

ALTER TABLE `sys_operation_log`
  MODIFY COLUMN `user_id` BIGINT UNSIGNED NULL COMMENT '操作人ID，匿名请求或已删除用户可为空';
