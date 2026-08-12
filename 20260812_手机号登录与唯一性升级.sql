USE `sja_system_db`;

-- 202608121431 - 增加了手机号登录所需的手机号唯一性约束。
-- 历史上不足或超过11位的占位手机号映射为 NULL，不影响已有数据升级。
ALTER TABLE `sys_user`
  ADD COLUMN `phone_unique` VARCHAR(20) GENERATED ALWAYS AS (
    CASE WHEN CHAR_LENGTH(TRIM(`phone`)) = 11 THEN TRIM(`phone`) ELSE NULL END
  ) STORED COMMENT '用于保证合法长度手机号唯一' AFTER `phone`,
  ADD UNIQUE KEY `uk_phone` (`phone_unique`);
