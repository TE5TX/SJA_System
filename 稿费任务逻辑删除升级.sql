-- 在已有数据库上执行一次，为五类稿费任务增加逻辑删除标记。
-- is_valid = 1 表示正常任务；is_valid = 0 表示已逻辑删除。

ALTER TABLE `biz_editorial_article`
  ADD COLUMN `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效' AFTER `audit_status`;

ALTER TABLE `biz_photography_work`
  ADD COLUMN `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效' AFTER `audit_status`;

ALTER TABLE `biz_video_work`
  ADD COLUMN `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效' AFTER `audit_status`;

ALTER TABLE `biz_visual_work`
  ADD COLUMN `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效' AFTER `audit_status`;

ALTER TABLE `biz_monthly_subsidy`
  ADD COLUMN `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效' AFTER `audit_status`;
