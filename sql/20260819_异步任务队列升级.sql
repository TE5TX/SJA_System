CREATE TABLE IF NOT EXISTS `sys_async_task_execution` (
  `task_id` VARCHAR(36) NOT NULL COMMENT '异步任务UUID',
  `task_type` VARCHAR(32) NOT NULL COMMENT 'PROFILE_UPDATE/FEE_SUBMIT',
  `owner_id` BIGINT UNSIGNED NOT NULL COMMENT '任务提交用户ID',
  `completed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据库事务完成时间',

  PRIMARY KEY (`task_id`),

  KEY `idx_async_task_owner_time` (`owner_id`, `completed_at`),

  CONSTRAINT `fk_async_task_execution_owner`
    FOREIGN KEY (`owner_id`)
    REFERENCES `sys_user` (`id`)
    ON DELETE RESTRICT,

  CONSTRAINT `chk_async_task_execution_type`
    CHECK (`task_type` IN ('PROFILE_UPDATE', 'FEE_SUBMIT'))
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='异步写入任务幂等执行标记';