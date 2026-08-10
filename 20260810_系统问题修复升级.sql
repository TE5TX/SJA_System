USE `sja_system_db`;

-- 202608101042 - 修复了首次改密、真实提交人和稿费标准配置缺少数据库结构与数据的问题。

DROP PROCEDURE IF EXISTS `sja_add_column_if_missing`;
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
DELIMITER ;

CALL `sja_add_column_if_missing`('sys_user', 'must_change_password',
  'ALTER TABLE `sys_user` ADD COLUMN `must_change_password` TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''首次登录是否必须修改密码'' AFTER `password`');
CALL `sja_add_column_if_missing`('biz_editorial_article', 'submitter_id',
  'ALTER TABLE `biz_editorial_article` ADD COLUMN `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT ''实际提交人ID'' AFTER `id`');
CALL `sja_add_column_if_missing`('biz_photography_work', 'submitter_id',
  'ALTER TABLE `biz_photography_work` ADD COLUMN `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT ''实际提交人ID'' AFTER `id`');
CALL `sja_add_column_if_missing`('biz_video_work', 'submitter_id',
  'ALTER TABLE `biz_video_work` ADD COLUMN `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT ''实际提交人ID'' AFTER `id`');
CALL `sja_add_column_if_missing`('biz_visual_work', 'submitter_id',
  'ALTER TABLE `biz_visual_work` ADD COLUMN `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT ''实际提交人ID'' AFTER `id`');
CALL `sja_add_column_if_missing`('biz_monthly_subsidy', 'submitter_id',
  'ALTER TABLE `biz_monthly_subsidy` ADD COLUMN `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT ''实际提交人ID'' AFTER `id`');
DROP PROCEDURE `sja_add_column_if_missing`;

UPDATE `biz_editorial_article` w SET w.`submitter_id` = (
  SELECT c.`user_id` FROM `biz_work_contributor` c
  WHERE c.`work_type` = 'EDITORIAL' AND c.`work_id` = w.`id` ORDER BY c.`id` LIMIT 1
) WHERE w.`submitter_id` IS NULL;
UPDATE `biz_photography_work` w SET w.`submitter_id` = (
  SELECT c.`user_id` FROM `biz_work_contributor` c
  WHERE c.`work_type` = 'PHOTO' AND c.`work_id` = w.`id` ORDER BY c.`id` LIMIT 1
) WHERE w.`submitter_id` IS NULL;
UPDATE `biz_video_work` w SET w.`submitter_id` = (
  SELECT c.`user_id` FROM `biz_work_contributor` c
  WHERE c.`work_type` = 'VIDEO' AND c.`work_id` = w.`id` ORDER BY c.`id` LIMIT 1
) WHERE w.`submitter_id` IS NULL;
UPDATE `biz_visual_work` w SET w.`submitter_id` = (
  SELECT c.`user_id` FROM `biz_work_contributor` c
  WHERE c.`work_type` = 'VISUAL' AND c.`work_id` = w.`id` ORDER BY c.`id` LIMIT 1
) WHERE w.`submitter_id` IS NULL;
UPDATE `biz_monthly_subsidy` w SET w.`submitter_id` = (
  SELECT c.`user_id` FROM `biz_work_contributor` c
  WHERE c.`work_type` = 'SUBSIDY' AND c.`work_id` = w.`id` ORDER BY c.`id` LIMIT 1
) WHERE w.`submitter_id` IS NULL;

CREATE TABLE IF NOT EXISTS `biz_fee_standard` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_code` VARCHAR(30) NOT NULL COMMENT '部门代码',
  `platform` VARCHAR(20) NOT NULL COMMENT '平台',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类别',
  `rule_key` VARCHAR(50) NOT NULL COMMENT '规则键',
  `rule_value` DECIMAL(10, 4) NOT NULL COMMENT '规则值',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '规则描述',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-是，0-否',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_standard` (`dept_code`, `platform`, `category`, `rule_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='稿费标准配置表';

INSERT INTO `biz_fee_standard`
(`dept_code`, `platform`, `category`, `rule_key`, `rule_value`, `description`)
VALUES
('EDITORIAL','校报','原创头版头条','PER_1000_WORDS_FEE',100,'每1000字稿费'),
('EDITORIAL','校报','原创一般稿件','PER_1000_WORDS_FEE',80,'每1000字稿费'),
('EDITORIAL','校报','重新编辑类稿件','PER_1000_WORDS_FEE',50,'每1000字稿费'),
('EDITORIAL','院网','莲峰群英','PER_ARTICLE_FEE',120,'每篇稿费'),
('EDITORIAL','院网','学校新闻（含学校要闻、校园时讯）','PER_ARTICLE_FEE',50,'每篇稿费'),
('EDITORIAL','微信','半原创类','PER_PERSON_FEE',20,'每人稿费'),
('WECHAT','微信','原创类','READ_MIDDLE_THRESHOLD',6000,'中档阅读量阈值（超过）'),
('WECHAT','微信','原创类','READ_HIGH_THRESHOLD',10000,'高档阅读量阈值（超过）'),
('WECHAT','微信','原创类','READ_BASE_FEE',30,'基础每人稿费'),
('WECHAT','微信','原创类','READ_MIDDLE_FEE',50,'中档每人稿费'),
('WECHAT','微信','原创类','READ_HIGH_FEE',80,'高档每人稿费'),
('PHOTO','院网','','COUNT_1_FEE',30,'选用1张每人稿费'),
('PHOTO','院网','','COUNT_2_TO_5_FEE',40,'选用2至5张每人稿费'),
('PHOTO','院网','','COUNT_OVER_5_FEE',60,'选用6张及以上每人稿费'),
('PHOTO','校报','','COUNT_1_FEE',20,'选用1张每人稿费'),
('PHOTO','校报','','COUNT_2_TO_5_FEE',30,'选用2至5张每人稿费'),
('PHOTO','校报','','COUNT_OVER_5_FEE',40,'选用6张及以上每人稿费'),
('VIDEO','','活动新闻类视频','BASE_FEE',400,'每篇基础稿费'),
('VIDEO','','活动新闻类视频','SHOOT_RATIO',0.7,'拍摄分配比例'),
('VIDEO','','风景类系列短视频','BASE_FEE',150,'每篇基础稿费'),
('VIDEO','','风景类系列短视频','SHOOT_RATIO',0.5,'拍摄分配比例'),
('VIDEO','','原创类短视频','BASE_FEE',400,'每篇基础稿费'),
('VIDEO','','原创类短视频','SHOOT_RATIO',0.5,'拍摄分配比例'),
('VIDEO','','原创类长视频','BASE_FEE',600,'每篇基础稿费'),
('VIDEO','','原创类长视频','SHOOT_RATIO',0.6,'拍摄分配比例'),
('VIDEO','','','LIKE_THRESHOLD',200,'点赞奖励起始值'),
('VIDEO','','','LIKE_STEP',100,'点赞奖励步长'),
('VIDEO','','','LIKE_BONUS',50,'每档点赞奖励'),
('VIDEO','','','REPRINT_BONUS',100,'官方转载奖励'),
('VIDEO','','','COMPLEX_EDIT_BONUS',20,'复杂剪辑奖励'),
('VISUAL','微信','普通消息类','PER_PERSON_FEE',15,'每人稿费'),
('VISUAL','微信','原创美文（不含制图）','PER_PERSON_FEE',20,'每人稿费'),
('VISUAL','微信','原创美文（含部分制图）','PER_PERSON_FEE',30,'每人稿费'),
('VISUAL','微信','互动、长图、SVG动图、手绘','PER_PERSON_FEE',50,'每人稿费'),
('VISUAL','校园网','校园网首页大图','PER_IMAGE_FEE',40,'每幅稿费'),
('MANAGEMENT','月度补贴','微信统筹编辑','PER_PERSON_FEE',80,'每人每月补贴'),
('MANAGEMENT','月度补贴','排版编辑','PER_PERSON_FEE',80,'每人每月补贴'),
('MANAGEMENT','月度补贴','校对','PER_PERSON_FEE',10,'每人每月补贴'),
('MANAGEMENT','月度补贴','主席团成员','PER_PERSON_FEE',150,'每人每月补贴'),
('MANAGEMENT','月度补贴','校园巡查组长','PER_PERSON_FEE',80,'每人每月补贴'),
('MANAGEMENT','月度补贴','校园巡查组员','PER_PERSON_FEE',50,'每人每月补贴')
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `is_active` = 1;
