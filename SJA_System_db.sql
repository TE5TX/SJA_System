-- 警告：本脚本用于完整重置，会永久删除 sja_system_db 中的现有数据。
-- 202608101117 - 修复了初始化脚本只创建缺失对象、无法真正重置数据库的问题。
DROP DATABASE IF EXISTS `sja_system_db`;
CREATE DATABASE IF NOT EXISTS `sja_system_db` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE `sja_system_db`;

-- ==========================================
-- 1. 基础管理模块 (Base Management)
-- ==========================================

-- 1.1 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称（如：管理员、部长、实习部员）',
  `role_code` INT NOT NULL COMMENT '角色代码（唯一标识）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.2 部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `dept_code` VARCHAR(30) NOT NULL COMMENT '部门代码',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 1.3 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `must_change_password` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '首次登录是否必须修改密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `gender` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `photo` VARCHAR(255) DEFAULT NULL COMMENT '照片URL',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `student_no` VARCHAR(20) NOT NULL COMMENT '学号',
  `college` VARCHAR(100) DEFAULT NULL COMMENT '二级学院',
  `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级',
  `dorm_address` VARCHAR(100) DEFAULT NULL COMMENT '宿舍地址',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `bank_card` VARCHAR(30) DEFAULT NULL COMMENT '银行卡号',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_student_no` (`student_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.4 用户-角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role_id` BIGINT UNSIGNED NOT NULL,
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 1.5 用户-部门关联表
CREATE TABLE IF NOT EXISTS `sys_user_dept` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `dept_id` BIGINT UNSIGNED NOT NULL,
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dept` (`user_id`, `dept_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户部门关联表';


-- ==========================================
-- 2. 稿费业务模块 (Fee Calculation)
-- ==========================================

-- 2.1 采编部稿件表
CREATE TABLE IF NOT EXISTS `biz_editorial_article` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '实际提交人ID',
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `platform` VARCHAR(20) NOT NULL COMMENT '平台：微信/院网/校报',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类别',
  `word_count` INT DEFAULT 0 COMMENT '字数',
  `read_count` INT DEFAULT 0 COMMENT '阅读量',
  `publish_date` DATE NULL COMMENT '发布日期，未发布时为空',
  `article_link` VARCHAR(500) DEFAULT NULL COMMENT '文章链接',
  `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
  `is_special_node` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否特殊节点：1-是，0-否',
  `special_multiplier` TINYINT NOT NULL DEFAULT 1 COMMENT '特殊节点倍数：普通1倍，特殊节点2倍或3倍',
  `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '文章总稿费',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态：SUBMITTED-待审核，APPROVED-已通过，REJECTED-已驳回',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采编部稿件表';

-- 2.2 摄影部作品表
CREATE TABLE IF NOT EXISTS `biz_photography_work` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '实际提交人ID',
  `title` VARCHAR(255) NOT NULL COMMENT '作品标题',
  `platform` VARCHAR(20) NOT NULL COMMENT '平台：微信/院网/校报',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类别（微信展示，计算不使用）',
  `photo_count` INT DEFAULT 0 COMMENT '选中图片数',
  `read_count` INT DEFAULT 0 COMMENT '阅读量(微信用)',
  `publish_date` DATE NULL COMMENT '发布日期，未发布时为空',
  `article_link` VARCHAR(500) DEFAULT NULL COMMENT '作品链接',
  `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
  `is_special_node` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否特殊节点：1-是，0-否',
  `special_multiplier` TINYINT NOT NULL DEFAULT 1 COMMENT '特殊节点倍数：普通1倍，特殊节点2倍或3倍',
  `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '作品总稿费',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态：SUBMITTED-待审核，APPROVED-已通过，REJECTED-已驳回',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄影部作品表';

-- 2.3 视频部作品表
CREATE TABLE IF NOT EXISTS `biz_video_work` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '实际提交人ID',
  `video_title` VARCHAR(255) NOT NULL COMMENT '视频标题',
  `category` VARCHAR(50) NOT NULL COMMENT '类别：风景类系列短视频/活动新闻类视频/原创类短视频/原创类长视频',
  `video_duration_sec` INT DEFAULT 0 COMMENT '视频时长(秒)',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `reprint_status` VARCHAR(50) DEFAULT '无' COMMENT '转载情况',
  `is_complex_edit` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用复杂剪辑手段：1-是，0-否',
  `publish_date` DATE NULL COMMENT '发布日期，未发布时为空',
  `video_link` VARCHAR(500) DEFAULT NULL COMMENT '视频链接',
  `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
  `is_special_node` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否特殊节点：1-是，0-否',
  `special_multiplier` TINYINT NOT NULL DEFAULT 1 COMMENT '特殊节点倍数：普通1倍，特殊节点2倍或3倍',
  `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '视频总稿费',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态：SUBMITTED-待审核，APPROVED-已通过，REJECTED-已驳回',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频部作品表';

-- 2.4 视觉部作品表
CREATE TABLE IF NOT EXISTS `biz_visual_work` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '实际提交人ID',
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类别',
  `author_count` INT DEFAULT 0 COMMENT '参与人数',
  `author_avg_fee` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '作者稿费均额',
  `publish_date` DATE NULL COMMENT '发布日期，未发布时为空',
  `article_link` VARCHAR(500) DEFAULT NULL COMMENT '文章链接',
  `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
  `is_special_node` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否特殊节点：1-是，0-否',
  `special_multiplier` TINYINT NOT NULL DEFAULT 1 COMMENT '特殊节点倍数：普通1倍，特殊节点2倍或3倍',
  `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '总金额',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态：SUBMITTED-待审核，APPROVED-已通过，REJECTED-已驳回',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视觉部作品表';

-- 2.5 月度编辑、管理及巡查补贴表
CREATE TABLE IF NOT EXISTS `biz_monthly_subsidy` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `submitter_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '实际提交人ID',
  `title` VARCHAR(255) NOT NULL COMMENT '补贴任务标题',
  `category` VARCHAR(50) NOT NULL COMMENT '补贴类型',
  `subsidy_month` VARCHAR(7) NOT NULL COMMENT '补贴月份：yyyy-MM',
  `total_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '补贴总金额',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态：SUBMITTED-待审核，APPROVED-已通过，REJECTED-已驳回',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度编辑、管理及巡查补贴表';

-- 2.6 统一的稿费人员分配表
CREATE TABLE IF NOT EXISTS `biz_work_contributor` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `work_type` VARCHAR(20) NOT NULL COMMENT '任务类型：EDITORIAL-采编, PHOTO-摄影, VIDEO-视频, VISUAL-视觉, SUBSIDY-月度补贴',
  `work_id` BIGINT UNSIGNED NOT NULL COMMENT '对应作品表的ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  `role_in_work` VARCHAR(20) NOT NULL COMMENT '角色：AUTHOR-作者, SHOOTER-拍摄, EDITOR-剪辑, DESIGNER-设计, MEMBER-补贴人员',
  `fee_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '该成员最终应得稿费',
  `work_count` INT DEFAULT NULL COMMENT '该人员对应作品数量（摄影院网/校报、校园网首页大图使用）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_user` (`work_type`, `work_id`, `user_id`, `role_in_work`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一稿费人员分配表';

-- 2.7 稿费标准配置表
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
-- 202608101635 - 修复了院网摄影选图数量分档及配置键不一致的问题。
('PHOTO','院网','','COUNT_UP_TO_2_FEE',30,'选用2张及以下每人稿费'),
('PHOTO','院网','','COUNT_3_TO_4_FEE',40,'选用3至4张每人稿费'),
('PHOTO','院网','','COUNT_5_PLUS_FEE',60,'选用5张及以上每人稿费'),
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


-- ==========================================
-- 3. 权限与日志模块 (Permission & Log)
-- ==========================================

-- 3.1 菜单/权限资源表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单/按钮名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限标识 (如: fee:export)',
  `menu_type` TINYINT(1) NOT NULL COMMENT '类型：1-目录, 2-菜单, 3-按钮',
  `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父级ID',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_valid` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有效',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 3.2 角色-菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 3.3 操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '操作人ID',
  `real_name` VARCHAR(50) NOT NULL COMMENT '操作人姓名',
  `role_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人当时的角色',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型 (如: 审核稿费, 修改个人信息)',
  `target_module` VARCHAR(50) NOT NULL COMMENT '操作模块 (如: 视频部稿费, 用户管理)',
  `target_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作对象的ID',
  `detail` TEXT DEFAULT NULL COMMENT '操作详情/修改前后的数据对比(JSON格式)',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ==========================================
-- 4. 基础数据初始化 (Initial Data)
-- ==========================================

-- 4.1 插入角色数据 (sys_role)
INSERT INTO `sys_role` (`role_name`, `role_code`, `sort_order`, `is_valid`, `remark`) VALUES
-- 核心管理层
('主席', 1, 1, 1, '最高权限，可查看/修改所有人信息'),
('老师', 2, 2, 1, '指导老师，可查看所有人信息及已审核的稿费、操作日志'),
('秘书长', 3, 3, 1, '负责审核明细表、导出月度稿费发放表、管理所有用户信息及查看日志'),

-- 部门管理层
('部长', 4, 10, 1, '各部门负责人，负责提交每月的稿费明细表'),

-- 采编部细分角色
('微信统筹编辑', 5, 20, 1, '采编部微信统筹负责人'),
('排版编辑', 6, 21, 1, '负责微信公众号文章排版'),
('校对', 7, 22, 1, '负责文章错别字及内容校对'),

-- 实习与小队角色
('实习部员（一轮）', 8, 30, 1, '第一轮招新实习部员'),
('实习部员（二轮）', 9, 31, 1, '第二轮招新实习部员'),
('留任部员', 10, 32, 1, '实习期满留任的正式部员'),
('校园小队队长', 11, 33, 1, '校园小队负责人'),
('校园小队队员', 12, 34, 1, '校园小队成员'),

-- 其他角色
('游客', 13, 99, 1, '未登录或无具体职务的访客，仅拥有基础查看权限');


-- 4.2 插入部门数据 (sys_dept)
INSERT INTO `sys_dept` (`dept_name`, `dept_code`, `sort_order`, `is_valid`, `remark`) VALUES
('主席团', 'PRESIDIUM', 1, 1, '组织最高决策与管理团队'),
('采编部', 'EDITORIAL', 2, 1, '负责文章撰写、编辑、排版与校对'),
('摄影部', 'PHOTOGRAPHY', 3, 1, '负责活动现场拍摄与后期修图'),
('视觉部', 'VISUAL', 4, 1, '负责海报设计、视觉排版与美工'),
('视频部', 'VIDEO', 5, 1, '负责视频策划、拍摄与剪辑');


-- ==========================================
-- 5. 菜单权限初始化 (Menu & Permission)
-- ==========================================

-- 5.1 插入菜单/权限数据 (sys_menu)
-- menu_type: 1-目录, 2-菜单, 3-按钮
-- 注意：permission_code 有唯一约束，目录用 nav: 前缀，菜单用 page: 前缀，按钮用实际权限码

-- ===== 一级目录 (type=1) =====
INSERT INTO `sys_menu` (`id`, `menu_name`, `permission_code`, `menu_type`, `parent_id`, `path`, `sort_order`, `is_valid`, `remark`) VALUES
(1,  '个人管理', 'nav:personal', 1, 0, '/user',         1, 1, '个人信息查看与修改'),
(3,  '稿费管理', 'nav:fee',      1, 0, '/fee',          2, 1, '稿费提交、审核、导出'),
(8,  '系统管理', 'nav:system',   1, 0, '/system',       3, 1, '人员、角色、部门、日志管理');

-- ===== 素拓工具 (type=2，直属顶级) =====
INSERT INTO `sys_menu` (`id`, `menu_name`, `permission_code`, `menu_type`, `parent_id`, `path`, `sort_order`, `is_valid`, `remark`) VALUES
(40, '素拓工具', 'page:sujia', 2, 0, '/tools/sujia', 4, 1, '素拓名单生成工具');

-- ===== 个人管理 子菜单 (type=2) 与按钮 (type=3) =====
INSERT INTO `sys_menu` (`id`, `menu_name`, `permission_code`, `menu_type`, `parent_id`, `path`, `sort_order`, `is_valid`, `remark`) VALUES
(2,  '个人信息',   'page:profile',       2, 1, '/user/profile',  1, 1, '查看与编辑个人信息'),
(10, '查看个人信息', 'user:profile:view',  3, 2, NULL, 1, 1, '查看个人信息按钮'),
(11, '编辑个人信息', 'user:profile:edit',  3, 2, NULL, 2, 1, '编辑手机号、宿舍地址、银行卡号'),
(12, '修改密码',     'user:password:edit', 3, 2, NULL, 3, 1, '修改登录密码');

-- ===== 稿费管理 子菜单 (type=2) 与按钮 (type=3) =====
INSERT INTO `sys_menu` (`id`, `menu_name`, `permission_code`, `menu_type`, `parent_id`, `path`, `sort_order`, `is_valid`, `remark`) VALUES
(4, '稿费提交', 'page:fee:submit', 2, 3, '/fee/submit', 1, 1, '部长提交稿费明细'),
(5, '稿费审核', 'page:fee:audit',  2, 3, '/fee/audit',  2, 1, '秘书长审核稿费明细'),
(6, '稿费导出', 'page:fee:export', 2, 3, '/fee/export', 3, 1, '导出稿费 Excel 对账单'),
(7, '稿费列表', 'page:fee:list',   2, 3, '/fee/list',   4, 1, '查看稿费明细列表'),
(20, '提交稿费', 'fee:submit', 3, 4, NULL, 1, 1, '提交稿费按钮'),
(24, '选择稿费作者', 'fee:contributors:view', 3, 4, NULL, 2, 1, '查询有效人员的姓名和学号'),
(25, '编辑稿费任务', 'fee:edit', 3, 4, NULL, 3, 1, '编辑未审核的稿费任务'),
(21, '审核稿费', 'fee:audit',  3, 5, NULL, 1, 1, '审核通过/驳回按钮'),
(22, '导出稿费', 'fee:export', 3, 6, NULL, 1, 1, '导出 Excel 按钮'),
(23, '查看稿费', 'fee:view',   3, 7, NULL, 1, 1, '查看稿费列表/详情按钮');

-- ===== 系统管理 子菜单 (type=2) 与按钮 (type=3) =====
INSERT INTO `sys_menu` (`id`, `menu_name`, `permission_code`, `menu_type`, `parent_id`, `path`, `sort_order`, `is_valid`, `remark`) VALUES
(9,  '人员管理', 'page:sys:users', 2, 8, '/system/users', 1, 1, '全部人员信息查看与编辑'),
(32, '角色管理', 'page:sys:roles', 2, 8, '/system/roles', 2, 1, '角色列表查看'),
(34, '部门管理', 'page:sys:depts', 2, 8, '/system/depts', 3, 1, '部门列表查看'),
(36, '菜单管理', 'page:sys:menus', 2, 8, '/system/menus', 4, 1, '菜单权限树查看'),
(38, '日志管理', 'page:sys:logs',  2, 8, '/system/logs',  5, 1, '操作日志查看'),
(30, '查看人员', 'sys:users:view', 3, 9,  NULL, 1, 1, '查看人员列表按钮'),
(31, '编辑人员', 'sys:users:edit', 3, 9,  NULL, 2, 1, '编辑用户信息按钮'),
(33, '查看角色', 'sys:role:view',  3, 32, NULL, 1, 1, '查看角色列表按钮'),
(35, '查看部门', 'sys:dept:view',  3, 34, NULL, 1, 1, '查看部门列表按钮'),
(37, '查看菜单', 'sys:menu:view',  3, 36, NULL, 1, 1, '查看菜单树按钮'),
(39, '查看日志', 'sys:log:view',   3, 38, NULL, 1, 1, '查看操作日志按钮');

-- ===== 素拓工具 按钮 (type=3) =====
INSERT INTO `sys_menu` (`id`, `menu_name`, `permission_code`, `menu_type`, `parent_id`, `path`, `sort_order`, `is_valid`, `remark`) VALUES
(41, '使用素拓工具', 'tools:sujia:use', 3, 40, NULL, 1, 1, '素拓名单生成按钮');


-- ==========================================
-- 6. 角色-菜单权限关联 (Role-Menu Mapping)
-- ==========================================
-- 角色 ID 对照（来自 sys_role 插入顺序）：
--   1=主席, 2=老师, 3=秘书长, 4=部长
--   5=微信统筹编辑, 6=排版编辑, 7=校对
--   8=实习部员(一轮), 9=实习部员(二轮), 10=留任部员
--   11=校园小队队长, 12=校园小队队员, 13=游客

-- ===== 主席 (role_id=1)：全部权限 =====
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(1, 1,  '个人管理-目录'),
(1, 2,  '个人信息-菜单'),
(1, 10, '查看个人信息'),
(1, 11, '编辑个人信息'),
(1, 12, '修改密码'),
(1, 3,  '稿费管理-目录'),
(1, 4,  '稿费提交-菜单'),
(1, 20, '提交稿费按钮'),
(1, 24, '选择稿费作者'),
(1, 5,  '稿费审核-菜单'),
(1, 21, '审核稿费按钮'),
(1, 6,  '稿费导出-菜单'),
(1, 22, '导出稿费按钮'),
(1, 7,  '稿费列表-菜单'),
(1, 23, '查看稿费按钮'),
(1, 8,  '系统管理-目录'),
(1, 9,  '人员管理-菜单'),
(1, 30, '查看人员'),
(1, 31, '编辑人员'),
(1, 32, '角色管理-菜单'),
(1, 33, '查看角色'),
(1, 34, '部门管理-菜单'),
(1, 35, '查看部门'),
(1, 36, '菜单管理-菜单'),
(1, 37, '查看菜单'),
(1, 38, '日志管理-菜单'),
(1, 39, '查看日志'),
(1, 40, '素拓工具-菜单'),
(1, 41, '使用素拓工具');

-- ===== 老师 (role_id=2)：个人信息 + 人员管理 + 稿费查看 + 日志查看 + 素拓 =====
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(2, 1,  '个人管理-目录'),
(2, 2,  '个人信息-菜单'),
(2, 10, '查看个人信息'),
(2, 11, '编辑个人信息'),
(2, 12, '修改密码'),
(2, 3,  '稿费管理-目录'),
(2, 7,  '稿费列表-菜单'),
(2, 23, '查看稿费按钮'),
(2, 8,  '系统管理-目录'),
(2, 9,  '人员管理-菜单'),
(2, 30, '查看人员'),
(2, 31, '编辑人员'),
(2, 32, '角色管理-菜单'),
(2, 33, '查看角色'),
(2, 34, '部门管理-菜单'),
(2, 35, '查看部门'),
(2, 38, '日志管理-菜单'),
(2, 39, '查看日志'),
(2, 40, '素拓工具-菜单'),
(2, 41, '使用素拓工具');

-- ===== 秘书长 (role_id=3)：稿费全部 + 人员管理 + 系统查看 + 素拓 =====
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(3, 1,  '个人管理-目录'),
(3, 2,  '个人信息-菜单'),
(3, 10, '查看个人信息'),
(3, 11, '编辑个人信息'),
(3, 12, '修改密码'),
(3, 3,  '稿费管理-目录'),
(3, 4,  '稿费提交-菜单'),
(3, 20, '提交稿费按钮'),
(3, 24, '选择稿费作者'),
(3, 25, '编辑稿费任务'),
(3, 5,  '稿费审核-菜单'),
(3, 21, '审核稿费按钮'),
(3, 6,  '稿费导出-菜单'),
(3, 22, '导出稿费按钮'),
(3, 7,  '稿费列表-菜单'),
(3, 23, '查看稿费按钮'),
(3, 8,  '系统管理-目录'),
(3, 9,  '人员管理-菜单'),
(3, 30, '查看人员'),
(3, 31, '编辑人员'),
(3, 32, '角色管理-菜单'),
(3, 33, '查看角色'),
(3, 34, '部门管理-菜单'),
(3, 35, '查看部门'),
(3, 36, '菜单管理-菜单'),
(3, 37, '查看菜单'),
(3, 38, '日志管理-菜单'),
(3, 39, '查看日志'),
(3, 40, '素拓工具-菜单'),
(3, 41, '使用素拓工具');

-- ===== 部长 (role_id=4)：个人信息 + 稿费提交/查看 + 素拓 =====
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(4, 1,  '个人管理-目录'),
(4, 2,  '个人信息-菜单'),
(4, 10, '查看个人信息'),
(4, 11, '编辑个人信息'),
(4, 12, '修改密码'),
(4, 3,  '稿费管理-目录'),
(4, 4,  '稿费提交-菜单'),
(4, 20, '提交稿费按钮'),
(4, 24, '选择稿费作者'),
(4, 25, '编辑未审核稿费任务'),
(4, 7,  '稿费列表-菜单'),
(4, 23, '查看稿费按钮'),
(4, 40, '素拓工具-菜单'),
(4, 41, '使用素拓工具');

-- ===== 采编部细分角色 (role_id=5,6,7)：个人信息 + 素拓 =====
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(5, 1,  '个人管理-目录'),
(5, 2,  '个人信息-菜单'),
(5, 10, '查看个人信息'),
(5, 11, '编辑个人信息'),
(5, 12, '修改密码'),
(5, 40, '素拓工具-菜单'),
(5, 41, '使用素拓工具');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(6, 1,  '个人管理-目录'),
(6, 2,  '个人信息-菜单'),
(6, 10, '查看个人信息'),
(6, 11, '编辑个人信息'),
(6, 12, '修改密码'),
(6, 40, '素拓工具-菜单'),
(6, 41, '使用素拓工具');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(7, 1,  '个人管理-目录'),
(7, 2,  '个人信息-菜单'),
(7, 10, '查看个人信息'),
(7, 11, '编辑个人信息'),
(7, 12, '修改密码'),
(7, 40, '素拓工具-菜单'),
(7, 41, '使用素拓工具');

-- ===== 实习部员 & 留任部员 & 小队角色 (role_id=8~12)：个人信息 + 素拓 =====
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(8, 1,  '个人管理-目录'),
(8, 2,  '个人信息-菜单'),
(8, 10, '查看个人信息'),
(8, 11, '编辑个人信息'),
(8, 12, '修改密码'),
(8, 40, '素拓工具-菜单'),
(8, 41, '使用素拓工具');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(9, 1,  '个人管理-目录'),
(9, 2,  '个人信息-菜单'),
(9, 10, '查看个人信息'),
(9, 11, '编辑个人信息'),
(9, 12, '修改密码'),
(9, 40, '素拓工具-菜单'),
(9, 41, '使用素拓工具');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(10, 1,  '个人管理-目录'),
(10, 2,  '个人信息-菜单'),
(10, 10, '查看个人信息'),
(10, 11, '编辑个人信息'),
(10, 12, '修改密码'),
(10, 40, '素拓工具-菜单'),
(10, 41, '使用素拓工具');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(11, 1,  '个人管理-目录'),
(11, 2,  '个人信息-菜单'),
(11, 10, '查看个人信息'),
(11, 11, '编辑个人信息'),
(11, 12, '修改密码'),
(11, 40, '素拓工具-菜单'),
(11, 41, '使用素拓工具');

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(12, 1,  '个人管理-目录'),
(12, 2,  '个人信息-菜单'),
(12, 10, '查看个人信息'),
(12, 11, '编辑个人信息'),
(12, 12, '修改密码'),
(12, 40, '素拓工具-菜单'),
(12, 41, '使用素拓工具');

-- ===== 游客 (role_id=13)：仅个人信息基础查看 =====
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`) VALUES
(13, 1,  '个人管理-目录'),
(13, 2,  '个人信息-菜单'),
(13, 10, '查看个人信息');
