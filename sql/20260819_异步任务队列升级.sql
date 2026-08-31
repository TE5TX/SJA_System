-- 202608192148 - 修复了异步消费者故障恢复时可能重复执行数据库写入的问题。
-- 执行方式：mysql -u root -p sja_system_db < sql/20260819_异步任务队列升级.sql

CREATE TABLE IF NOT EXISTS `sys_async_task_execution` (
  `task_id` VARCHAR(36) NOT NULL COMMENT '异步任务UUID',
  `task_type` VARCHAR(32) NOT NULL COMMENT 'PROFILE_UPDATE/FEE_SUBMIT',
  `owner_id` BIGINT NOT NULL COMMENT '任务提交用户ID',
  `completed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据库事务完成时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_async_task_owner_time` (`owner_id`, `completed_at`),
  CONSTRAINT `fk_async_task_execution_owner`
    FOREIGN KEY (`owner_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_async_task_execution_type`
    CHECK (`task_type` IN ('PROFILE_UPDATE', 'FEE_SUBMIT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='异步写入任务幂等执行标记';

-- 该表只保存任务ID和执行时间，不保存个人资料或稿费正文。
-- 建议定期清理180天前的记录，但不要清理仍可能被队列重放的近期记录：
-- DELETE FROM `sys_async_task_execution` WHERE `completed_at` < NOW() - INTERVAL 180 DAY;
