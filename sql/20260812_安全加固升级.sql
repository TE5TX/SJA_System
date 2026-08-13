USE `sja_system_db`;

-- 现有数据库安全升级脚本。可重复执行；执行前请先备份数据库。
-- 如果身份证存在重复值，脚本会主动中止，避免静默覆盖数据。

DROP PROCEDURE IF EXISTS `sja_add_column_if_missing`;
DROP PROCEDURE IF EXISTS `sja_add_index_if_missing`;
DROP PROCEDURE IF EXISTS `sja_add_constraint_if_missing`;
DROP PROCEDURE IF EXISTS `sja_assert_id_card_unique`;
DROP PROCEDURE IF EXISTS `sja_assert_photo_unique`;

DELIMITER $$
CREATE PROCEDURE `sja_add_column_if_missing`(
  IN table_name_value VARCHAR(64),
  IN column_name_value VARCHAR(64),
  IN ddl_value TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = table_name_value
      AND column_name = column_name_value
  ) THEN
    SET @sja_ddl = ddl_value;
    PREPARE sja_stmt FROM @sja_ddl;
    EXECUTE sja_stmt;
    DEALLOCATE PREPARE sja_stmt;
  END IF;
END$$

CREATE PROCEDURE `sja_add_index_if_missing`(
  IN table_name_value VARCHAR(64),
  IN index_name_value VARCHAR(64),
  IN ddl_value TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = table_name_value
      AND index_name = index_name_value
  ) THEN
    SET @sja_ddl = ddl_value;
    PREPARE sja_stmt FROM @sja_ddl;
    EXECUTE sja_stmt;
    DEALLOCATE PREPARE sja_stmt;
  END IF;
END$$

CREATE PROCEDURE `sja_add_constraint_if_missing`(
  IN table_name_value VARCHAR(64),
  IN constraint_name_value VARCHAR(64),
  IN ddl_value TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = table_name_value
      AND constraint_name = constraint_name_value
  ) THEN
    SET @sja_ddl = ddl_value;
    PREPARE sja_stmt FROM @sja_ddl;
    EXECUTE sja_stmt;
    DEALLOCATE PREPARE sja_stmt;
  END IF;
END$$

CREATE PROCEDURE `sja_assert_id_card_unique`()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `sys_user`
    WHERE `id_card` IS NOT NULL AND TRIM(`id_card`) <> ''
    GROUP BY TRIM(`id_card`)
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'sys_user.id_card 存在重复值，请清理后重新执行安全升级脚本';
  END IF;
END$$

CREATE PROCEDURE `sja_assert_photo_unique`()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `sys_user`
    WHERE `photo` IS NOT NULL AND TRIM(`photo`) <> ''
    GROUP BY TRIM(`photo`)
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'sys_user.photo 存在重复路径，请清理后重新执行安全升级脚本';
  END IF;
END$$
DELIMITER ;

CALL `sja_add_column_if_missing`('sys_user', 'must_change_password',
  'ALTER TABLE `sys_user` ADD COLUMN `must_change_password` TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''首次登录是否必须修改密码'' AFTER `password`');
CALL `sja_add_column_if_missing`('sys_user', 'token_version',
  'ALTER TABLE `sys_user` ADD COLUMN `token_version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''令牌版本，修改密码/权限或注销时递增'' AFTER `must_change_password`');

CALL `sja_add_column_if_missing`('sys_operation_log', 'request_id',
  'ALTER TABLE `sys_operation_log` ADD COLUMN `request_id` VARCHAR(64) DEFAULT NULL COMMENT ''请求追踪ID'' AFTER `ip_address`');
CALL `sja_add_column_if_missing`('sys_operation_log', 'request_method',
  'ALTER TABLE `sys_operation_log` ADD COLUMN `request_method` VARCHAR(10) DEFAULT NULL COMMENT ''HTTP方法'' AFTER `request_id`');
CALL `sja_add_column_if_missing`('sys_operation_log', 'request_uri',
  'ALTER TABLE `sys_operation_log` ADD COLUMN `request_uri` VARCHAR(255) DEFAULT NULL COMMENT ''请求路径'' AFTER `request_method`');
CALL `sja_add_column_if_missing`('sys_operation_log', 'result_status',
  'ALTER TABLE `sys_operation_log` ADD COLUMN `result_status` VARCHAR(20) NOT NULL DEFAULT ''SUCCESS'' COMMENT ''结果：SUCCESS/FAILURE'' AFTER `request_uri`');
CALL `sja_add_column_if_missing`('sys_operation_log', 'before_data',
  'ALTER TABLE `sys_operation_log` ADD COLUMN `before_data` TEXT DEFAULT NULL COMMENT ''变更前数据（已脱敏）'' AFTER `result_status`');
CALL `sja_add_column_if_missing`('sys_operation_log', 'after_data',
  'ALTER TABLE `sys_operation_log` ADD COLUMN `after_data` TEXT DEFAULT NULL COMMENT ''变更后数据（已脱敏）'' AFTER `before_data`');
CALL `sja_add_column_if_missing`('sys_operation_log', 'user_agent',
  'ALTER TABLE `sys_operation_log` ADD COLUMN `user_agent` VARCHAR(500) DEFAULT NULL COMMENT ''客户端User-Agent'' AFTER `after_data`');
CALL `sja_add_column_if_missing`('sys_operation_log', 'duration_ms',
  'ALTER TABLE `sys_operation_log` ADD COLUMN `duration_ms` BIGINT UNSIGNED DEFAULT NULL COMMENT ''请求耗时毫秒'' AFTER `user_agent`');

CREATE TABLE IF NOT EXISTS `sys_refresh_token` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `token_id` VARCHAR(64) NOT NULL COMMENT 'JWT jti',
  `token_hash` CHAR(64) NOT NULL COMMENT '刷新令牌SHA-256摘要',
  `token_family` VARCHAR(64) NOT NULL COMMENT '轮换令牌家族ID',
  `user_id` BIGINT UNSIGNED NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `revoked_at` DATETIME DEFAULT NULL,
  `replaced_by` VARCHAR(64) DEFAULT NULL COMMENT '轮换后的token_id',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_id` (`token_id`),
  KEY `idx_refresh_user_active` (`user_id`, `revoked_at`, `expires_at`),
  KEY `idx_refresh_family_active` (`token_family`, `revoked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可轮换刷新令牌表';

CALL `sja_add_column_if_missing`('sys_refresh_token', 'token_family',
  'ALTER TABLE `sys_refresh_token` ADD COLUMN `token_family` VARCHAR(64) DEFAULT NULL COMMENT ''轮换令牌家族ID'' AFTER `token_hash`');
UPDATE `sys_refresh_token` SET `token_family` = `token_id`
WHERE `token_family` IS NULL OR TRIM(`token_family`) = '';
ALTER TABLE `sys_refresh_token` MODIFY COLUMN `token_family` VARCHAR(64) NOT NULL COMMENT '轮换令牌家族ID';
CALL `sja_add_index_if_missing`('sys_refresh_token', 'idx_refresh_family_active',
  'ALTER TABLE `sys_refresh_token` ADD KEY `idx_refresh_family_active` (`token_family`, `revoked_at`)');

-- 空身份证保留为 NULL，多个 NULL 不违反唯一约束。
UPDATE `sys_user` SET `id_card` = NULL WHERE `id_card` IS NOT NULL AND TRIM(`id_card`) = '';
UPDATE `sys_user`
SET `photo` = REPLACE(`photo`, '/uploads/avatars/', '/api/v1/upload/avatar/')
WHERE `photo` LIKE '/uploads/avatars/%';
CALL `sja_assert_id_card_unique`();
CALL `sja_assert_photo_unique`();
CALL `sja_add_index_if_missing`('sys_user', 'uk_id_card',
  'ALTER TABLE `sys_user` ADD UNIQUE KEY `uk_id_card` (`id_card`)');
CALL `sja_add_index_if_missing`('sys_user', 'uk_photo',
  'ALTER TABLE `sys_user` ADD UNIQUE KEY `uk_photo` (`photo`)');
CALL `sja_add_index_if_missing`('sys_role_menu', 'idx_role_menu_menu',
  'ALTER TABLE `sys_role_menu` ADD KEY `idx_role_menu_menu` (`menu_id`)');
CALL `sja_add_index_if_missing`('biz_editorial_article', 'idx_editorial_submitter',
  'ALTER TABLE `biz_editorial_article` ADD KEY `idx_editorial_submitter` (`submitter_id`)');
CALL `sja_add_index_if_missing`('biz_photography_work', 'idx_photo_submitter',
  'ALTER TABLE `biz_photography_work` ADD KEY `idx_photo_submitter` (`submitter_id`)');
CALL `sja_add_index_if_missing`('biz_video_work', 'idx_video_submitter',
  'ALTER TABLE `biz_video_work` ADD KEY `idx_video_submitter` (`submitter_id`)');
CALL `sja_add_index_if_missing`('biz_visual_work', 'idx_visual_submitter',
  'ALTER TABLE `biz_visual_work` ADD KEY `idx_visual_submitter` (`submitter_id`)');
CALL `sja_add_index_if_missing`('biz_monthly_subsidy', 'idx_subsidy_submitter',
  'ALTER TABLE `biz_monthly_subsidy` ADD KEY `idx_subsidy_submitter` (`submitter_id`)');
CALL `sja_add_index_if_missing`('sys_operation_log', 'idx_log_request_id',
  'ALTER TABLE `sys_operation_log` ADD KEY `idx_log_request_id` (`request_id`)');
CALL `sja_add_index_if_missing`('sys_operation_log', 'idx_log_module_target',
  'ALTER TABLE `sys_operation_log` ADD KEY `idx_log_module_target` (`target_module`, `target_id`)');

CALL `sja_add_constraint_if_missing`('sys_user_role', 'fk_user_role_user',
  'ALTER TABLE `sys_user_role` ADD CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE');
CALL `sja_add_constraint_if_missing`('sys_user_role', 'fk_user_role_role',
  'ALTER TABLE `sys_user_role` ADD CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE');
CALL `sja_add_constraint_if_missing`('sys_user_dept', 'fk_user_dept_user',
  'ALTER TABLE `sys_user_dept` ADD CONSTRAINT `fk_user_dept_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE');
CALL `sja_add_constraint_if_missing`('sys_user_dept', 'fk_user_dept_dept',
  'ALTER TABLE `sys_user_dept` ADD CONSTRAINT `fk_user_dept_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`) ON DELETE CASCADE');
CALL `sja_add_constraint_if_missing`('sys_role_menu', 'fk_role_menu_role',
  'ALTER TABLE `sys_role_menu` ADD CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE');
CALL `sja_add_constraint_if_missing`('sys_role_menu', 'fk_role_menu_menu',
  'ALTER TABLE `sys_role_menu` ADD CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE CASCADE');
CALL `sja_add_constraint_if_missing`('biz_work_contributor', 'fk_work_contributor_user',
  'ALTER TABLE `biz_work_contributor` ADD CONSTRAINT `fk_work_contributor_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT');
CALL `sja_add_constraint_if_missing`('biz_editorial_article', 'fk_editorial_submitter',
  'ALTER TABLE `biz_editorial_article` ADD CONSTRAINT `fk_editorial_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL');
CALL `sja_add_constraint_if_missing`('biz_photography_work', 'fk_photo_submitter',
  'ALTER TABLE `biz_photography_work` ADD CONSTRAINT `fk_photo_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL');
CALL `sja_add_constraint_if_missing`('biz_video_work', 'fk_video_submitter',
  'ALTER TABLE `biz_video_work` ADD CONSTRAINT `fk_video_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL');
CALL `sja_add_constraint_if_missing`('biz_visual_work', 'fk_visual_submitter',
  'ALTER TABLE `biz_visual_work` ADD CONSTRAINT `fk_visual_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL');
CALL `sja_add_constraint_if_missing`('biz_monthly_subsidy', 'fk_subsidy_submitter',
  'ALTER TABLE `biz_monthly_subsidy` ADD CONSTRAINT `fk_subsidy_submitter` FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL');
CALL `sja_add_constraint_if_missing`('sys_operation_log', 'fk_operation_log_user',
  'ALTER TABLE `sys_operation_log` ADD CONSTRAINT `fk_operation_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT');
CALL `sja_add_constraint_if_missing`('sys_refresh_token', 'fk_refresh_token_user',
  'ALTER TABLE `sys_refresh_token` ADD CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE');

CALL `sja_add_constraint_if_missing`('sys_user', 'chk_sys_user_gender',
  'ALTER TABLE `sys_user` ADD CONSTRAINT `chk_sys_user_gender` CHECK (`gender` IN (0, 1, 2))');
CALL `sja_add_constraint_if_missing`('sys_user', 'chk_sys_user_valid',
  'ALTER TABLE `sys_user` ADD CONSTRAINT `chk_sys_user_valid` CHECK (`is_valid` IN (0, 1))');
CALL `sja_add_constraint_if_missing`('sys_user', 'chk_sys_user_must_change_password',
  'ALTER TABLE `sys_user` ADD CONSTRAINT `chk_sys_user_must_change_password` CHECK (`must_change_password` IN (0, 1))');
CALL `sja_add_constraint_if_missing`('biz_work_contributor', 'chk_contributor_fee',
  'ALTER TABLE `biz_work_contributor` ADD CONSTRAINT `chk_contributor_fee` CHECK (`fee_amount` >= 0)');
CALL `sja_add_constraint_if_missing`('biz_work_contributor', 'chk_contributor_count',
  'ALTER TABLE `biz_work_contributor` ADD CONSTRAINT `chk_contributor_count` CHECK (`work_count` IS NULL OR `work_count` >= 1)');
CALL `sja_add_constraint_if_missing`('sys_operation_log', 'chk_operation_log_result',
  'ALTER TABLE `sys_operation_log` ADD CONSTRAINT `chk_operation_log_result` CHECK (`result_status` IN (''SUCCESS'', ''FAILURE''))');

CALL `sja_add_constraint_if_missing`('biz_editorial_article', 'chk_editorial_valid',
  'ALTER TABLE `biz_editorial_article` ADD CONSTRAINT `chk_editorial_valid` CHECK (`is_valid` IN (0, 1))');
CALL `sja_add_constraint_if_missing`('biz_editorial_article', 'chk_editorial_audit',
  'ALTER TABLE `biz_editorial_article` ADD CONSTRAINT `chk_editorial_audit` CHECK (`audit_status` IN (''SUBMITTED'', ''APPROVED'', ''REJECTED''))');
CALL `sja_add_constraint_if_missing`('biz_photography_work', 'chk_photo_valid',
  'ALTER TABLE `biz_photography_work` ADD CONSTRAINT `chk_photo_valid` CHECK (`is_valid` IN (0, 1))');
CALL `sja_add_constraint_if_missing`('biz_photography_work', 'chk_photo_audit',
  'ALTER TABLE `biz_photography_work` ADD CONSTRAINT `chk_photo_audit` CHECK (`audit_status` IN (''SUBMITTED'', ''APPROVED'', ''REJECTED''))');
CALL `sja_add_constraint_if_missing`('biz_video_work', 'chk_video_valid',
  'ALTER TABLE `biz_video_work` ADD CONSTRAINT `chk_video_valid` CHECK (`is_valid` IN (0, 1))');
CALL `sja_add_constraint_if_missing`('biz_video_work', 'chk_video_audit',
  'ALTER TABLE `biz_video_work` ADD CONSTRAINT `chk_video_audit` CHECK (`audit_status` IN (''SUBMITTED'', ''APPROVED'', ''REJECTED''))');
CALL `sja_add_constraint_if_missing`('biz_visual_work', 'chk_visual_valid',
  'ALTER TABLE `biz_visual_work` ADD CONSTRAINT `chk_visual_valid` CHECK (`is_valid` IN (0, 1))');
CALL `sja_add_constraint_if_missing`('biz_visual_work', 'chk_visual_audit',
  'ALTER TABLE `biz_visual_work` ADD CONSTRAINT `chk_visual_audit` CHECK (`audit_status` IN (''SUBMITTED'', ''APPROVED'', ''REJECTED''))');
CALL `sja_add_constraint_if_missing`('biz_monthly_subsidy', 'chk_subsidy_valid',
  'ALTER TABLE `biz_monthly_subsidy` ADD CONSTRAINT `chk_subsidy_valid` CHECK (`is_valid` IN (0, 1))');
CALL `sja_add_constraint_if_missing`('biz_monthly_subsidy', 'chk_subsidy_audit',
  'ALTER TABLE `biz_monthly_subsidy` ADD CONSTRAINT `chk_subsidy_audit` CHECK (`audit_status` IN (''SUBMITTED'', ''APPROVED'', ''REJECTED''))');

-- 主席、老师可查看稿费任务但不能审批、导出或查看审计日志。
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`id` = rm.`role_id`
JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
WHERE r.`role_code` = 1
  AND m.`permission_code` IN ('page:fee:audit', 'fee:audit', 'page:fee:export', 'fee:export',
                              'page:sys:logs', 'sys:log:view');

DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`id` = rm.`role_id`
JOIN `sys_menu` m ON m.`id` = rm.`menu_id`
WHERE r.`role_code` = 2
  AND m.`permission_code` IN ('page:sys:logs', 'sys:log:view');

DROP PROCEDURE `sja_assert_photo_unique`;
DROP PROCEDURE `sja_assert_id_card_unique`;
DROP PROCEDURE `sja_add_constraint_if_missing`;
DROP PROCEDURE `sja_add_index_if_missing`;
DROP PROCEDURE `sja_add_column_if_missing`;
