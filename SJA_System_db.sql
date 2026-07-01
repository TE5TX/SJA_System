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
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `platform` VARCHAR(20) NOT NULL COMMENT '平台：微信/院网/校报',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类别',
  `word_count` INT DEFAULT 0 COMMENT '字数',
  `read_count` INT DEFAULT 0 COMMENT '阅读量',
  `publish_date` DATE NOT NULL COMMENT '发布日期',
  `article_link` VARCHAR(500) DEFAULT NULL COMMENT '文章链接',
  `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
  `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '文章总稿费',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采编部稿件表';

-- 2.2 摄影部作品表
CREATE TABLE IF NOT EXISTS `biz_photography_work` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL COMMENT '作品标题',
  `platform` VARCHAR(20) NOT NULL COMMENT '平台：微信/院网/校报',
  `photo_count` INT DEFAULT 0 COMMENT '选中图片数',
  `read_count` INT DEFAULT 0 COMMENT '阅读量(微信用)',
  `publish_date` DATE NOT NULL COMMENT '发布日期',
  `article_link` VARCHAR(500) DEFAULT NULL COMMENT '作品链接',
  `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
  `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '作品总稿费',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摄影部作品表';

-- 2.3 视频部作品表
CREATE TABLE IF NOT EXISTS `biz_video_work` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `video_title` VARCHAR(255) NOT NULL COMMENT '视频标题',
  `category` VARCHAR(50) NOT NULL COMMENT '类别：风景/活动新闻/原创短/原创长',
  `video_duration_sec` INT DEFAULT 0 COMMENT '视频时长(秒)',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `reprint_status` VARCHAR(50) DEFAULT '无' COMMENT '转载情况',
  `publish_date` DATE NOT NULL COMMENT '发布日期',
  `video_link` VARCHAR(500) DEFAULT NULL COMMENT '视频链接',
  `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
  `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '视频总稿费',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频部作品表';

-- 2.4 视觉部作品表
CREATE TABLE IF NOT EXISTS `biz_visual_work` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '类别',
  `author_count` INT DEFAULT 0 COMMENT '参与人数',
  `author_avg_fee` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '作者稿费均额',
  `publish_date` DATE NOT NULL COMMENT '发布日期',
  `article_link` VARCHAR(500) DEFAULT NULL COMMENT '文章链接',
  `is_published` TINYINT(1) DEFAULT 0 COMMENT '是否已发布',
  `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '总金额',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视觉部作品表';

-- 2.5 统一的稿费人员分配表
CREATE TABLE IF NOT EXISTS `biz_work_contributor` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `work_type` VARCHAR(20) NOT NULL COMMENT '作品类型：EDITORIAL-采编, PHOTO-摄影, VIDEO-视频, VISUAL-视觉',
  `work_id` BIGINT UNSIGNED NOT NULL COMMENT '对应作品表的ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  `role_in_work` VARCHAR(20) NOT NULL COMMENT '角色：AUTHOR-作者, SHOOTER-拍摄, EDITOR-剪辑, DESIGNER-设计',
  `fee_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '该成员最终应得稿费',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_user` (`work_type`, `work_id`, `user_id`, `role_in_work`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一稿费人员分配表';

-- 2.6 稿费标准配置表
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