-- 202609281105 - 完成了风景类系列短视频和原创类短视频旧拍摄分配比例的停用。
-- 执行前请先备份 biz_fee_standard；仅停用规则，不删除历史记录或改写已保存稿费。
-- 应在新版本后端启用前执行，避免旧版本因回退到默认 50% 而继续按角色分配。
UPDATE `biz_fee_standard`
SET `is_active` = 0
WHERE `dept_code` = 'VIDEO'
  AND `platform` = ''
  AND `category` IN ('风景类系列短视频', '原创类短视频')
  AND `rule_key` = 'SHOOT_RATIO'
  AND `is_active` = 1;

-- 回滚（仅在同时回滚到旧版后端时执行；不会自动覆盖人工调整的规则值）：
-- UPDATE `biz_fee_standard`
-- SET `is_active` = 1
-- WHERE `dept_code` = 'VIDEO'
--   AND `platform` = ''
--   AND `category` IN ('风景类系列短视频', '原创类短视频')
--   AND `rule_key` = 'SHOOT_RATIO'
--   AND `is_active` = 0;
