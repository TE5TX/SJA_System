USE `sja_system_db`;

-- 202608121145 - 修复了未发布稿费任务无法保存空发布日期的问题。
ALTER TABLE `biz_editorial_article`
  MODIFY COLUMN `publish_date` DATE NULL COMMENT '发布日期，未发布时为空';

ALTER TABLE `biz_photography_work`
  MODIFY COLUMN `publish_date` DATE NULL COMMENT '发布日期，未发布时为空';

ALTER TABLE `biz_visual_work`
  MODIFY COLUMN `publish_date` DATE NULL COMMENT '发布日期，未发布时为空';

ALTER TABLE `biz_video_work`
  MODIFY COLUMN `publish_date` DATE NULL COMMENT '发布日期，未发布时为空';
