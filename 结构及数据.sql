/*
 Navicat Premium Dump SQL

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : sja_system_db

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 22/07/2026 09:57:04
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for biz_editorial_article
-- ----------------------------
DROP TABLE IF EXISTS `biz_editorial_article`;
CREATE TABLE `biz_editorial_article`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文章标题',
  `platform` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '平台：微信/院网/校报',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类别',
  `word_count` int NULL DEFAULT 0 COMMENT '字数',
  `read_count` int NULL DEFAULT 0 COMMENT '阅读量',
  `publish_date` date NOT NULL COMMENT '发布日期',
  `article_link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文章链接',
  `is_published` tinyint(1) NULL DEFAULT 0 COMMENT '是否已发布',
  `is_special_node` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否特殊节点：1-是，0-否',
  `special_multiplier` tinyint NOT NULL DEFAULT 1 COMMENT '特殊节点倍数：普通1倍，特殊节点2倍或3倍',
  `total_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '文章总稿费',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采编部稿件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of biz_editorial_article
-- ----------------------------

-- ----------------------------
-- Table structure for biz_fee_standard
-- ----------------------------
DROP TABLE IF EXISTS `biz_fee_standard`;
CREATE TABLE `biz_fee_standard`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门代码',
  `platform` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '平台',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类别',
  `rule_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则键',
  `rule_value` decimal(10, 4) NOT NULL COMMENT '规则值',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规则描述',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-是，0-否',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_standard`(`dept_code` ASC, `platform` ASC, `category` ASC, `rule_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '稿费标准配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of biz_fee_standard
-- ----------------------------

-- ----------------------------
-- Table structure for biz_photography_work
-- ----------------------------
DROP TABLE IF EXISTS `biz_photography_work`;
CREATE TABLE `biz_photography_work`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作品标题',
  `platform` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '平台：微信/院网/校报',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类别（微信展示，计算不使用）',
  `photo_count` int NULL DEFAULT 0 COMMENT '选中图片数',
  `read_count` int NULL DEFAULT 0 COMMENT '阅读量(微信用)',
  `publish_date` date NOT NULL COMMENT '发布日期',
  `article_link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作品链接',
  `is_published` tinyint(1) NULL DEFAULT 0 COMMENT '是否已发布',
  `is_special_node` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否特殊节点：1-是，0-否',
  `special_multiplier` tinyint NOT NULL DEFAULT 1 COMMENT '特殊节点倍数：普通1倍，特殊节点2倍或3倍',
  `total_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '作品总稿费',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '摄影部作品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of biz_photography_work
-- ----------------------------

-- ----------------------------
-- Table structure for biz_video_work
-- ----------------------------
DROP TABLE IF EXISTS `biz_video_work`;
CREATE TABLE `biz_video_work`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `video_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '视频标题',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类别：风景类系列短视频/活动新闻类视频/原创类短视频/原创类长视频',
  `video_duration_sec` int NULL DEFAULT 0 COMMENT '视频时长(秒)',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `reprint_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '无' COMMENT '转载情况',
  `is_complex_edit` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否使用复杂剪辑手段：1-是，0-否',
  `publish_date` date NOT NULL COMMENT '发布日期',
  `video_link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '视频链接',
  `is_published` tinyint(1) NULL DEFAULT 0 COMMENT '是否已发布',
  `is_special_node` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否特殊节点：1-是，0-否',
  `special_multiplier` tinyint NOT NULL DEFAULT 1 COMMENT '特殊节点倍数：普通1倍，特殊节点2倍或3倍',
  `total_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '视频总稿费',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视频部作品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of biz_video_work
-- ----------------------------

-- ----------------------------
-- Table structure for biz_visual_work
-- ----------------------------
DROP TABLE IF EXISTS `biz_visual_work`;
CREATE TABLE `biz_visual_work`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文章标题',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类别',
  `author_count` int NULL DEFAULT 0 COMMENT '参与人数',
  `author_avg_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '作者稿费均额',
  `publish_date` date NOT NULL COMMENT '发布日期',
  `article_link` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文章链接',
  `is_published` tinyint(1) NULL DEFAULT 0 COMMENT '是否已发布',
  `is_special_node` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否特殊节点：1-是，0-否',
  `special_multiplier` tinyint NOT NULL DEFAULT 1 COMMENT '特殊节点倍数：普通1倍，特殊节点2倍或3倍',
  `total_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '总金额',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视觉部作品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of biz_visual_work
-- ----------------------------

-- ----------------------------
-- Table structure for biz_monthly_subsidy
-- ----------------------------
DROP TABLE IF EXISTS `biz_monthly_subsidy`;
CREATE TABLE `biz_monthly_subsidy`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '补贴任务标题',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '补贴类型',
  `subsidy_month` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '补贴月份：yyyy-MM',
  `total_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '补贴总金额',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT '审核状态',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '月度编辑、管理及巡查补贴表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of biz_monthly_subsidy
-- ----------------------------

-- ----------------------------
-- Table structure for biz_work_contributor
-- ----------------------------
DROP TABLE IF EXISTS `biz_work_contributor`;
CREATE TABLE `biz_work_contributor`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `work_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型：EDITORIAL-采编, PHOTO-摄影, VIDEO-视频, VISUAL-视觉, SUBSIDY-月度补贴',
  `work_id` bigint UNSIGNED NOT NULL COMMENT '对应作品表的ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '关联 sys_user.id',
  `role_in_work` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：AUTHOR-作者, SHOOTER-拍摄, EDITOR-剪辑, DESIGNER-设计, MEMBER-补贴人员',
  `fee_amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '该成员最终应得稿费',
  `work_count` int NULL DEFAULT NULL COMMENT '该人员对应作品数量（摄影院网/校报、校园网首页大图使用）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_work_user`(`work_type` ASC, `work_id` ASC, `user_id` ASC, `role_in_work` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '统一稿费人员分配表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of biz_work_contributor
-- ----------------------------

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `dept_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门代码',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dept_code`(`dept_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, '主席团', 'PRESIDIUM', 1, 1, '组织最高决策与管理团队', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_dept` VALUES (2, '采编部', 'EDITORIAL', 2, 1, '负责文章撰写、编辑、排版与校对', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_dept` VALUES (3, '摄影部', 'PHOTOGRAPHY', 3, 1, '负责活动现场拍摄与后期修图', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_dept` VALUES (4, '视觉部', 'VISUAL', 4, 1, '负责海报设计、视觉排版与美工', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_dept` VALUES (5, '视频部', 'VIDEO', 5, 1, '负责视频策划、拍摄与剪辑', '2026-07-12 12:56:52', '2026-07-12 12:56:52');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单/按钮名称',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限标识 (如: fee:export)',
  `menu_type` tinyint(1) NOT NULL COMMENT '类型：1-目录, 2-菜单, 3-按钮',
  `parent_id` bigint UNSIGNED NULL DEFAULT 0 COMMENT '父级ID',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '前端路由路径',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_permission_code`(`permission_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '个人管理', 'nav:personal', 1, 0, '/user', 1, 1, '个人信息查看与修改', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (2, '个人信息', 'page:profile', 2, 1, '/user/profile', 1, 1, '查看与编辑个人信息', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (3, '稿费管理', 'nav:fee', 1, 0, '/fee', 2, 1, '稿费提交、审核、导出', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (4, '稿费提交', 'page:fee:submit', 2, 3, '/fee/submit', 1, 1, '部长提交稿费明细', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (5, '稿费审核', 'page:fee:audit', 2, 3, '/fee/audit', 2, 1, '秘书长审核稿费明细', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (6, '稿费导出', 'page:fee:export', 2, 3, '/fee/export', 3, 1, '导出稿费 Excel 对账单', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (7, '稿费列表', 'page:fee:list', 2, 3, '/fee/list', 4, 1, '查看稿费明细列表', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (8, '系统管理', 'nav:system', 1, 0, '/system', 3, 1, '人员、角色、部门、日志管理', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (9, '人员管理', 'page:sys:users', 2, 8, '/system/users', 1, 1, '全部人员信息查看与编辑', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (10, '查看个人信息', 'user:profile:view', 3, 2, NULL, 1, 1, '查看个人信息按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (11, '编辑个人信息', 'user:profile:edit', 3, 2, NULL, 2, 1, '编辑手机号、宿舍地址、银行卡号', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (12, '修改密码', 'user:password:edit', 3, 2, NULL, 3, 1, '修改登录密码', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (20, '提交稿费', 'fee:submit', 3, 4, NULL, 1, 1, '提交稿费按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (24, '选择稿费作者', 'fee:contributors:view', 3, 4, NULL, 2, 1, '查询有效人员的姓名和学号', '2026-07-22 00:00:00', '2026-07-22 00:00:00');
INSERT INTO `sys_menu` VALUES (25, '编辑稿费任务', 'fee:edit', 3, 4, NULL, 3, 1, '编辑未审核的稿费任务', '2026-07-22 00:00:00', '2026-07-22 00:00:00');
INSERT INTO `sys_menu` VALUES (21, '审核稿费', 'fee:audit', 3, 5, NULL, 1, 1, '审核通过/驳回按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (22, '导出稿费', 'fee:export', 3, 6, NULL, 1, 1, '导出 Excel 按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (23, '查看稿费', 'fee:view', 3, 7, NULL, 1, 1, '查看稿费列表/详情按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (30, '查看人员', 'sys:users:view', 3, 9, NULL, 1, 1, '查看人员列表按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (31, '编辑人员', 'sys:users:edit', 3, 9, NULL, 2, 1, '编辑用户信息按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (32, '角色管理', 'page:sys:roles', 2, 8, '/system/roles', 2, 1, '角色列表查看', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (33, '查看角色', 'sys:role:view', 3, 32, NULL, 1, 1, '查看角色列表按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (34, '部门管理', 'page:sys:depts', 2, 8, '/system/depts', 3, 1, '部门列表查看', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (35, '查看部门', 'sys:dept:view', 3, 34, NULL, 1, 1, '查看部门列表按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (36, '菜单管理', 'page:sys:menus', 2, 8, '/system/menus', 4, 1, '菜单权限树查看', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (37, '查看菜单', 'sys:menu:view', 3, 36, NULL, 1, 1, '查看菜单树按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (38, '日志管理', 'page:sys:logs', 2, 8, '/system/logs', 5, 1, '操作日志查看', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (39, '查看日志', 'sys:log:view', 3, 38, NULL, 1, 1, '查看操作日志按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (40, '素拓工具', 'page:sujia', 2, 0, '/tools/sujia', 4, 1, '素拓名单生成工具', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_menu` VALUES (41, '使用素拓工具', 'tools:sujia:use', 3, 40, NULL, 1, 1, '素拓名单生成按钮', '2026-07-12 12:56:52', '2026-07-12 12:56:52');

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '操作人ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作人姓名',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人当时的角色',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型 (如: 审核稿费, 修改个人信息)',
  `target_module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作模块 (如: 视频部稿费, 用户管理)',
  `target_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '操作对象的ID',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '操作详情/修改前后的数据对比(JSON格式)',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作IP地址',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------
INSERT INTO `sys_operation_log` VALUES (1, 1, 'admin', '0', 'UPDATE', 'USER', 2, '修改用户信息: realName [黄义鉴]->[黄义鉴]; photo [null]->[]; idCard [445322200411300030]->[445322200411300030]; gender updated; phone [18033372534]->[18033372534]; college [计算机学院]->[计算机学院]; className [23计科F班]->[23计科F班]; dormAddress [18栋223]->[18栋223]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-12 12:57:58');
INSERT INTO `sys_operation_log` VALUES (2, 1, 'admin', '0', 'UPDATE', 'USER', 1, '修改用户信息: realName [系统管理员]->[系统管理员]; photo [null]->[]; idCard [null]->[]; gender updated; phone [null]->[]; college [null]->[]; className [null]->[]; dormAddress [null]->[]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-12 15:22:22');
INSERT INTO `sys_operation_log` VALUES (3, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '上传照片: 陈灿群，原照片 [null]，新照片 [/uploads/avatars/1a65faef-eb2e-4c9b-892b-2731eb1076b4.png]', '', NULL, '2026-07-18 23:31:51');
INSERT INTO `sys_operation_log` VALUES (4, 56, '陈灿群', '', 'UPDATE', 'USER', 56, '更新个人资料，修改字段: bankCard;', '', NULL, '2026-07-18 23:31:57');
INSERT INTO `sys_operation_log` VALUES (5, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '删除照片: 陈灿群，原照片 [/uploads/avatars/1a65faef-eb2e-4c9b-892b-2731eb1076b4.png]，新照片 [null]', '', NULL, '2026-07-18 23:37:21');
INSERT INTO `sys_operation_log` VALUES (6, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '上传照片: 陈灿群，原照片 [null]，新照片 [/uploads/avatars/f3d1aa91-6d5c-400a-9461-bb35a26b72a4.png]', '', NULL, '2026-07-18 23:37:28');
INSERT INTO `sys_operation_log` VALUES (7, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '删除照片: 陈灿群，原照片 [/uploads/avatars/f3d1aa91-6d5c-400a-9461-bb35a26b72a4.png]，新照片 [null]', '', NULL, '2026-07-18 23:37:35');
INSERT INTO `sys_operation_log` VALUES (8, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '上传照片: 陈灿群，原照片 [null]，新照片 [/uploads/avatars/509e3813-39c0-499a-8438-1f644d1f36ac.png]', '', NULL, '2026-07-18 23:39:42');
INSERT INTO `sys_operation_log` VALUES (9, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '删除照片: 陈灿群，原照片 [/uploads/avatars/509e3813-39c0-499a-8438-1f644d1f36ac.png]，新照片 [null]', '', NULL, '2026-07-18 23:40:04');
INSERT INTO `sys_operation_log` VALUES (10, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '上传照片: 陈灿群，原照片 [null]，新照片 [/uploads/avatars/81e7f99f-49e7-48cc-a9c9-9a011d9e6dcf.png]', '', NULL, '2026-07-18 23:40:09');
INSERT INTO `sys_operation_log` VALUES (11, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '删除照片: 陈灿群，原照片 [/uploads/avatars/81e7f99f-49e7-48cc-a9c9-9a011d9e6dcf.png]，新照片 [null]', '', NULL, '2026-07-18 23:40:31');
INSERT INTO `sys_operation_log` VALUES (12, 56, '陈灿群', '', 'UPDATE', 'USER', 56, '更新个人资料，修改字段: photo;', '', NULL, '2026-07-18 23:41:22');
INSERT INTO `sys_operation_log` VALUES (13, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '上传照片: 陈灿群，原照片 []，新照片 [/uploads/avatars/21b42b45-e18d-4743-9930-28d884f5e999.png]', '', NULL, '2026-07-18 23:43:41');
INSERT INTO `sys_operation_log` VALUES (14, 56, '2024050402004', '13', 'UPDATE', 'USER', 56, '删除照片: 陈灿群，原照片 [/uploads/avatars/21b42b45-e18d-4743-9930-28d884f5e999.png]，新照片 [null]', '', NULL, '2026-07-18 23:43:43');
INSERT INTO `sys_operation_log` VALUES (15, 16, '2024050402040', '13', 'UPDATE', 'USER', 16, '上传照片: 刘韵，原照片 [null]，新照片 [/uploads/avatars/9778b299-ce4e-468c-961d-a18444e92cac.png]', '', NULL, '2026-07-22 09:13:28');
INSERT INTO `sys_operation_log` VALUES (16, 16, '2024050402040', '13', 'UPDATE', 'USER', 16, '删除照片: 刘韵，原照片 [/uploads/avatars/9778b299-ce4e-468c-961d-a18444e92cac.png]，新照片 [null]', '', NULL, '2026-07-22 09:13:31');
INSERT INTO `sys_operation_log` VALUES (17, 16, '2024050402040', '13', 'UPDATE', 'USER', 16, '上传照片: 刘韵，原照片 [null]，新照片 [/uploads/avatars/def68127-5255-480d-9941-20e516bb19bf.png]', '', NULL, '2026-07-22 09:22:30');
INSERT INTO `sys_operation_log` VALUES (18, 16, '2024050402040', '13', 'UPDATE', 'USER', 16, '删除照片: 刘韵，原照片 [/uploads/avatars/def68127-5255-480d-9941-20e516bb19bf.png]，新照片 [null]', '', NULL, '2026-07-22 09:22:33');
INSERT INTO `sys_operation_log` VALUES (19, 16, '2024050402040', '13', 'UPDATE', 'USER', 16, '上传照片: 刘韵，原照片 [null]，新照片 [/uploads/avatars/11ca6ec7-e13c-4bd0-81d0-19cd40ffe34c.png]', '', NULL, '2026-07-22 09:22:37');
INSERT INTO `sys_operation_log` VALUES (20, 16, '2024050402040', '13', 'UPDATE', 'USER', 16, '删除照片: 刘韵，原照片 [/uploads/avatars/11ca6ec7-e13c-4bd0-81d0-19cd40ffe34c.png]，新照片 [null]', '', NULL, '2026-07-22 09:22:39');
INSERT INTO `sys_operation_log` VALUES (21, 94, '杜美婷', '', 'UPDATE', 'USER', 94, '更新个人资料，修改字段: photo; bankCard;', '', NULL, '2026-07-22 09:25:48');
INSERT INTO `sys_operation_log` VALUES (22, 1, 'admin', '0', 'UPDATE', 'USER', 89, '修改用户信息: realName [王滢]->[王滢]; photo [null]->[]; idCard [441900200507121329]->[441900200507121329]; gender updated; phone [13532347206]->[13532347206]; college [人文社会科学学院]->[人文社会科学学院]; className [23新闻B班]->[23新闻B班]; dormAddress [凤翔3-409]->[凤翔3-409]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:36:10');
INSERT INTO `sys_operation_log` VALUES (23, 1, 'admin', '0', 'UPDATE', 'USER', 90, '修改用户信息: realName [龚楚淇]->[龚楚淇]; photo [null]->[]; idCard [440103200410274841]->[440103200410274841]; gender updated; phone [13680185067]->[13680185067]; college [管理学院]->[管理学院]; className [23财务管理C班]->[23财务管理C班]; dormAddress [凤翔4栋205]->[凤翔4栋205]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:36:43');
INSERT INTO `sys_operation_log` VALUES (24, 1, 'admin', '0', 'UPDATE', 'USER', 88, '修改用户信息: realName [魏黛琳]->[魏黛琳]; photo [null]->[]; idCard [445202200406188020]->[445202200406188020]; gender updated; phone [13822097152]->[13822097152]; college [人文社会科学学院]->[人文社会科学学院]; className [23新闻B班]->[23新闻B班]; dormAddress [3栋404]->[3栋404]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:36:58');
INSERT INTO `sys_operation_log` VALUES (25, 1, 'admin', '0', 'UPDATE', 'USER', 91, '修改用户信息: realName [何锌颖]->[何锌颖]; photo [null]->[]; idCard [44532220030214522X]->[44532220030214522X]; gender updated; phone [15818150965]->[15818150965]; college [外国语学院]->[外国语学院]; className [23英语B班]->[23英语B班]; dormAddress [朝晖523]->[朝晖523]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:37:09');
INSERT INTO `sys_operation_log` VALUES (26, 1, 'admin', '0', 'UPDATE', 'USER', 92, '修改用户信息: realName [梁衡]->[梁衡]; photo [null]->[]; idCard [440803200503312913]->[440803200503312913]; gender updated; phone [13790971716]->[13790971716]; college [人文社会科学学院]->[人文社会科学学院]; className [23新闻D班]->[23新闻D班]; dormAddress [8栋602]->[8栋602]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:37:20');
INSERT INTO `sys_operation_log` VALUES (27, 1, 'admin', '0', 'UPDATE', 'USER', 93, '修改用户信息: realName [张静文]->[张静文]; photo [null]->[]; idCard [440783200411150626]->[440783200411150626]; gender updated; phone [13066260393]->[13066260393]; college [人文社会科学学院]->[人文社会科学学院]; className [23新闻D班]->[23新闻D班]; dormAddress [凤翔3栋606]->[凤翔3栋606]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:37:33');
INSERT INTO `sys_operation_log` VALUES (28, 1, 'admin', '0', 'UPDATE', 'USER', 94, '修改用户信息: realName [杜美婷]->[杜美婷]; photo []->[]; idCard [411729200412257262]->[411729200412257262]; gender updated; phone [15814668266]->[15814668266]; college [人文社会科学学院]->[人文社会科学学院]; className [23新闻D班]->[23新闻D班]; dormAddress [凤翔3栋608]->[凤翔3栋608]; bankCard [1]->[1]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:38:08');
INSERT INTO `sys_operation_log` VALUES (29, 1, 'admin', '0', 'UPDATE', 'USER', 95, '修改用户信息: realName [郑诗亭]->[郑诗亭]; photo [null]->[]; idCard [445202200503063027]->[445202200503063027]; gender updated; phone [19840899982]->[19840899982]; college [人文社会科学学院]->[人文社会科学学院]; className [23法学a]->[23法学a]; dormAddress [香晖b1205]->[香晖b1205]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:38:25');
INSERT INTO `sys_operation_log` VALUES (30, 1, 'admin', '0', 'UPDATE', 'USER', 96, '修改用户信息: realName [钟利灵]->[钟利灵]; photo [null]->[]; idCard [441621200506260992]->[441621200506260992]; gender updated; phone [18475684548]->[18475684548]; college [人文社会科学学院]->[人文社会科学学院]; className [23新闻B班]->[23新闻B班]; dormAddress [凤翔3-408]->[凤翔3-408]; bankCard [null]->[]; remark updated; isValid updated; deptIds updated; roleIds updated; ', '', NULL, '2026-07-22 09:38:55');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称（如：管理员、部长、实习部员）',
  `role_code` int NOT NULL COMMENT '角色代码（唯一标识）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效，0-无效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '主席', 1, 1, 1, '最高权限，可查看/修改所有人信息', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (2, '老师', 2, 2, 1, '指导老师，可查看所有人信息及已审核的稿费、操作日志', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (3, '秘书长', 3, 3, 1, '负责审核明细表、导出月度稿费发放表、管理所有用户信息及查看日志', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (4, '部长', 4, 10, 1, '各部门负责人，负责提交每月的稿费明细表', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (5, '微信统筹编辑', 5, 20, 1, '采编部微信统筹负责人', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (6, '排版编辑', 6, 21, 1, '负责微信公众号文章排版', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (7, '校对', 7, 22, 1, '负责文章错别字及内容校对', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (8, '实习部员（一轮）', 8, 30, 1, '第一轮招新实习部员', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (9, '实习部员（二轮）', 9, 31, 1, '第二轮招新实习部员', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (10, '留任部员', 10, 32, 1, '实习期满留任的正式部员', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (11, '校园小队队长', 11, 33, 1, '校园小队负责人', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (12, '校园小队队员', 12, 34, 1, '校园小队成员', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (13, '游客', 13, 99, 1, '未登录或无具体职务的访客，仅拥有基础查看权限', '2026-07-12 12:56:52', '2026-07-12 12:56:52');
INSERT INTO `sys_role` VALUES (14, '管理员', 0, 0, 1, 'system administrator', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_role` VALUES (15, '正式部员', 14, 35, 1, 'formal member from roster', '2026-07-12 12:57:07', '2026-07-12 12:57:07');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` bigint UNSIGNED NOT NULL COMMENT '角色ID',
  `menu_id` bigint UNSIGNED NOT NULL COMMENT '菜单ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_menu`(`role_id` ASC, `menu_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 186 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (2, 1, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (3, 1, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (4, 1, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (5, 1, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (6, 1, 3, '稿费管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (7, 1, 4, '稿费提交-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (8, 1, 20, '提交稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (9, 1, 5, '稿费审核-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (10, 1, 21, '审核稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (11, 1, 6, '稿费导出-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (12, 1, 22, '导出稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (13, 1, 7, '稿费列表-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (14, 1, 23, '查看稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (15, 1, 8, '系统管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (16, 1, 9, '人员管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (17, 1, 30, '查看人员', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (18, 1, 31, '编辑人员', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (19, 1, 32, '角色管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (20, 1, 33, '查看角色', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (21, 1, 34, '部门管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (22, 1, 35, '查看部门', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (23, 1, 36, '菜单管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (24, 1, 37, '查看菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (25, 1, 38, '日志管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (26, 1, 39, '查看日志', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (27, 1, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (28, 1, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (29, 2, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (30, 2, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (31, 2, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (32, 2, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (33, 2, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (34, 2, 3, '稿费管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (35, 2, 7, '稿费列表-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (36, 2, 23, '查看稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (37, 2, 8, '系统管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (38, 2, 9, '人员管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (39, 2, 30, '查看人员', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (40, 2, 31, '编辑人员', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (41, 2, 32, '角色管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (42, 2, 33, '查看角色', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (43, 2, 34, '部门管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (44, 2, 35, '查看部门', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (45, 2, 38, '日志管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (46, 2, 39, '查看日志', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (47, 2, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (48, 2, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (49, 3, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (50, 3, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (51, 3, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (52, 3, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (53, 3, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (54, 3, 3, '稿费管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (55, 3, 4, '稿费提交-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (56, 3, 20, '提交稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (57, 3, 5, '稿费审核-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (58, 3, 21, '审核稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (59, 3, 6, '稿费导出-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (60, 3, 22, '导出稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (61, 3, 7, '稿费列表-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (62, 3, 23, '查看稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (63, 3, 8, '系统管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (64, 3, 9, '人员管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (65, 3, 30, '查看人员', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (66, 3, 31, '编辑人员', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (67, 3, 32, '角色管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (68, 3, 33, '查看角色', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (69, 3, 34, '部门管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (70, 3, 35, '查看部门', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (71, 3, 36, '菜单管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (72, 3, 37, '查看菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (73, 3, 38, '日志管理-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (74, 3, 39, '查看日志', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (75, 3, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (76, 3, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (77, 4, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (78, 4, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (79, 4, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (80, 4, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (81, 4, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (82, 4, 3, '稿费管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (83, 4, 4, '稿费提交-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (84, 4, 20, '提交稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (85, 4, 7, '稿费列表-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (86, 4, 23, '查看稿费按钮', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (87, 4, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (88, 4, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (89, 5, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (90, 5, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (91, 5, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (92, 5, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (93, 5, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (94, 5, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (95, 5, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (96, 6, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (97, 6, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (98, 6, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (99, 6, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (100, 6, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (101, 6, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (102, 6, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (103, 7, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (104, 7, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (105, 7, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (106, 7, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (107, 7, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (108, 7, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (109, 7, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (110, 8, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (111, 8, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (112, 8, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (113, 8, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (114, 8, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (115, 8, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (116, 8, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (117, 9, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (118, 9, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (119, 9, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (120, 9, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (121, 9, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (122, 9, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (123, 9, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (124, 10, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (125, 10, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (126, 10, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (127, 10, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (128, 10, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (129, 10, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (130, 10, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (131, 11, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (132, 11, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (133, 11, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (134, 11, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (135, 11, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (136, 11, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (137, 11, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (138, 12, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (139, 12, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (140, 12, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (141, 12, 11, '编辑个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (142, 12, 12, '修改密码', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (143, 12, 40, '素拓工具-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (144, 12, 41, '使用素拓工具', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (145, 13, 1, '个人管理-目录', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (146, 13, 2, '个人信息-菜单', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (147, 13, 10, '查看个人信息', '2026-07-12 12:56:52');
INSERT INTO `sys_role_menu` VALUES (148, 14, 1, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (149, 14, 2, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (150, 14, 3, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (151, 14, 4, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (152, 14, 5, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (153, 14, 6, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (154, 14, 7, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (155, 14, 8, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (156, 14, 9, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (157, 14, 10, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (158, 14, 11, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (159, 14, 12, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (160, 14, 20, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (161, 14, 21, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (162, 14, 22, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (163, 14, 23, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (164, 14, 30, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (165, 14, 31, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (166, 14, 32, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (167, 14, 33, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (168, 14, 34, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (169, 14, 35, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (170, 14, 36, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (171, 14, 37, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (172, 14, 38, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (173, 14, 39, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (174, 14, 40, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (175, 14, 41, '管理员-全部权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (186, 1, 24, '选择稿费作者', '2026-07-22 00:00:00');
INSERT INTO `sys_role_menu` VALUES (187, 3, 24, '选择稿费作者', '2026-07-22 00:00:00');
INSERT INTO `sys_role_menu` VALUES (188, 4, 24, '选择稿费作者', '2026-07-22 00:00:00');
INSERT INTO `sys_role_menu` VALUES (189, 14, 24, '管理员-全部权限', '2026-07-22 00:00:00');
INSERT INTO `sys_role_menu` VALUES (190, 3, 25, '编辑稿费任务', '2026-07-22 00:00:00');
INSERT INTO `sys_role_menu` VALUES (191, 4, 25, '编辑未审核稿费任务', '2026-07-22 00:00:00');
INSERT INTO `sys_role_menu` VALUES (192, 14, 25, '管理员-全部权限', '2026-07-22 00:00:00');
INSERT INTO `sys_role_menu` VALUES (179, 15, 1, '正式部员-基础权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (180, 15, 2, '正式部员-基础权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (181, 15, 10, '正式部员-基础权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (182, 15, 11, '正式部员-基础权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (183, 15, 12, '正式部员-基础权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (184, 15, 40, '正式部员-基础权限', '2026-07-12 12:57:07');
INSERT INTO `sys_role_menu` VALUES (185, 15, 41, '正式部员-基础权限', '2026-07-12 12:57:07');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名（登录账号）',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '真实姓名',
  `gender` tinyint(1) NOT NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '照片URL',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `student_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学号',
  `college` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '二级学院',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '班级',
  `dorm_address` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '宿舍地址',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `bank_card` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行卡号',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否有效',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 156 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2b$10$uWpPds.1IbfpsD2.4rhsp./r5.1i0VxcJarfLlaWpQBgB7zxgmEqO', '系统管理员', 0, '', '', 'ADMIN', '', '', '', '', '', 1, '管理员', '2026-07-12 12:57:07', '2026-07-12 15:22:22');
INSERT INTO `sys_user` VALUES (2, '2023030103052', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '黄义鉴', 1, '', '445322200411300030', '2023030103052', '计算机学院', '23计科F班', '18栋223', '18033372534', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-12 12:57:58');
INSERT INTO `sys_user` VALUES (3, '2024050401003', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '蔡沐哲', 2, NULL, '445121200606211620', '2024050401003', '人文社会科学学院', '24新闻B班', '香晖A825', '17817688736', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (4, '2023070303020', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈美艳', 2, NULL, '440803200406053921', '2023070303020', '经贸学院', '23国贸C班', '香晖B1207', '18022686137', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (5, '2024070102016', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '郭潇远', 2, NULL, '410323200502089628', '2024070102016', '经贸学院', '24金融B班', '香晖C912', '18319424396', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (6, '2024070202009', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '韩佩桦', 2, NULL, '440801200512240621', '2024070202009', '人文社会科学学院', '24新闻A班', '香晖c703', '18927699239', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (7, '2024050301005', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '何嘉雨', 2, NULL, '440105200608014546', '2024050301005', '人文社会科学学院', '24新闻D班', '香晖A1128', '13640880032', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (8, '2024080405010', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '何秋雨', 2, NULL, '441781200602080526', '2024080405010', '人文社会科学学院', '24法学D班', '朝晖1129', '17875567183', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (9, '2024050402022', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '胡雅琪', 2, NULL, '330683200602065626', '2024050402022', '人文社会科学学院', '24新闻D班', '香晖A1124', '13588511814', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (10, '2024050402030', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '赖心敏', 2, NULL, '441323200610136029', '2024050402030', '人文社会科学学院', '24新闻C', '香晖A831', '13631149072', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (11, '2024060101023', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李依澄', 2, NULL, '442000200604011764', '2024060101023', '管理学院', '24财务管理A班', '香晖B523', '18024233233', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (12, '2024050401032', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '梁泳琳', 1, NULL, '440703200510286016', '2024050401032', '人文社会科学学院', '24新闻B班', '香晖A821', '19531473628', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (13, '2024050401034', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '林佳仪', 2, NULL, '445202200603120623', '2024050401034', '人文社会科学学院', '24新闻A', '香晖A817', '15820534446', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (14, '2024050201020', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '林嘉钰', 2, NULL, '440103200608273326', '2024050201020', '人文社会科学学院', '24行管A', '香晖A727', '19502060827', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (15, '2023080304017', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '刘诗怡', 2, NULL, '441324200409162344', '2023080304017', '外国语学院', '23商英D班', '朝晖426', '15398891461', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (16, '2024050402040', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '刘韵', 2, NULL, '411525200612037524', '2024050402040', '人文社会科学学院', '24新闻D', '香晖A1124', '17610556632', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-22 09:22:39');
INSERT INTO `sys_user` VALUES (17, '2024050401038', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陆爱珊', 2, NULL, '440106200604186929', '2024050401038', '人文社会科学学院', '24新闻A', '香晖A814', '13725325448', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (18, '2024050402050', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '韦雅萍', 2, NULL, '441781200602144681', '2024050402050', '人文社会科学学院', '24新闻D班', '香晖A1119', '13829878625', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (19, '2024050401056', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '幸美琪', 2, NULL, '441481200512296300', '2024050401056', '人文社会科学学院', '24新闻B班', '香晖A828', '15819985715', NULL, 1, '正式成员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (20, '2024050402054', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '鄢珂悦', 2, NULL, '420984200509133326', '2024050402054', '人文社会科学学院', '24新闻D班', '香晖A1120', '13642568597', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (21, '2024030101078', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '余梦洁', 2, NULL, '361130200604166224', '2024030101078', '计算机学院', '24计科A班', '香晖B402', '15967106959', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (22, '2024050402061', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张丽萍', 2, NULL, '362430200512161722', '2024050402061', '人文社会科学学院', '24新闻C班', '香晖A830', '15059850198', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (23, '2024050401064', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张宇桐', 2, NULL, '441302200607248722', '2024050401064', '人文社会科学学院', '24新闻A班', '香晖A819', '13794531748', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (24, '2023050401068', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '赵梦悠然', 2, NULL, '150723200510139224', '2023050401068', '人文社会科学学院', '23新闻B班', '凤翔3-410', '18925387267', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (25, '2024050401072', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '朱芷瑶', 2, NULL, '440106200510200929', '2024050401072', '人文社会科学学院', '24新闻B班', '香晖A821', '13169316472', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (26, '2023050401008', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈颖琪', 2, NULL, '440105200403137622', '2023050401008', '人文社会科学学院', '23新闻A', '凤翔3栋306', '15113391203', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (27, '2024040701012', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '杜嘉俊', 1, NULL, '440606200511040055', '2024040701012', '材料与食品学院', '24食安A', '19栋525', '13172331181', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (28, '2024050401022', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '黄渲淇', 2, NULL, '445121200509073123', '2024050401022', '人文社会科学学院', '24新闻B', '香晖A826', '13715715161', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (29, '2023050401074', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '黄莹', 2, NULL, '445302200411040624', '2023050401074', '人文社会科学学院', '23新闻B班', '凤翔3-406', '13826897475', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (30, '2023050402057', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '孔洁茹', 2, NULL, '441802200301062420', '2023050402057', '人文社会科学学院', '23新闻D班', '凤翔3栋610', '15816262304', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (31, '2023050402059', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李敏茹', 2, NULL, '441421200502204429', '2023050402059', '人文社会科学学院', '23新闻A', '凤翔3栋606', '13714393933', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (32, '2023080102025', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李欣悦', 2, NULL, '430281200508310284', '2023080102025', '外国语学院', '23翻译b班', '朝晖434', '18124126099', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (33, '2024090101018', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李泽翰', 1, NULL, '330902200507041018', '2024090101018', '艺术设计学院', '24产设A班', '10栋652', '15068018728', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (34, '2023050401083', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '林丽娜', 2, NULL, '441581200501142125', '2023050401083', '人文社会科学学院', '23新闻B班', '凤翔3-403', '13631126281', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (35, '2023050402067', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陆烁琳', 2, NULL, '445121200409225687', '2023050402067', '人文社会科学学院', '23法学F班', '凤翔3栋507', '13609665213', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (36, '2023050402028', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '罗艾如', 2, NULL, '440605200504300289', '2023050402028', '人文社会科学学院', '23新闻C班', '凤翔3栋502', '13690316769', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (37, '2024080405021', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '谭晓茵', 2, NULL, '440785200610023120', '2024080405021', '外国语学院', '24英语B', '朝晖1130', '18125495832', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (38, '2023010401044', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '王广宇', 1, NULL, '320724200512103071', '2023010401044', '电子信息学院', '23光电A班', '17栋148', '18552219838', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (39, '2024050103044', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '王熙颖', 2, NULL, '511622200606101921', '2024050103044', '人文社会科学学院', '24法学C班', '香晖A707', '13808862619', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (40, '2023070103009', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '王源至', 0, NULL, NULL, '2023070103009', '经贸学院', '23金融c班', '岐头17栋', '18807611302', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (41, '2024050401052', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '吴佩芹', 2, NULL, '440803200510022420', '2024050401052', '人文社会科学学院', '24新闻B班', '香晖A825', '18933820977', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (42, '2024060101020', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '肖文欣', 2, NULL, '441323200511058547', '2024060101020', '管理学院', '24财管A班', '香晖B523', '14776740883', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (43, '2023020301072', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '谢晓梅', 2, NULL, '441900200410291102', '2023020301072', '机电工程学院', '23自动化A2班', '香晖D1020', '13537341850', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (44, '2024050601012', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '徐铭浚', 1, NULL, '440111200511177514', '2024050601012', '电子信息学院', '24通信A', '17栋455', '19374982706', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (45, '2024030102085', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '詹一方', 1, NULL, '440305200510108254', '2024030102085', '计算机学院', '24计科C班', '16栋128', '13824722175', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (46, '2024080406024', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '湛楚', 2, NULL, '44098120050617002X', '2024080406024', '外国语学院', '24英语C班', '朝晖1136', '18476512388', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (47, '202308010125', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张思琪', 2, NULL, '350721200506062126', '202308010125', '外国语学院', '23翻译A班', '朝晖苑428', '17820309053', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (48, '2024040802035', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张镇涛', 1, NULL, '440105200607216613', '2024040802035', '材料与食品学院', '24应化C班', '19栋626', '14750051084', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (49, '2024020301036', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '郑培琪', 2, NULL, '440606200508160128', '2024020301036', '机电工程学院', '24自动化A', '香晖B515', '13360371871', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (50, '2024020302072', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '钟昊轩', 1, NULL, '445281200501086319', '2024020302072', '机电工程学院', '24自动化B2', '10栋626', '13128369329', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (51, '2024070301014', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '钟秀萍', 2, NULL, '44162120060407382X', '2024070301014', '人文社会科学学院', '24法学A', '香晖c807', '15811968661', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (52, '2024060401015', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '周可歆', 2, NULL, '440113200604250621', '2024060401015', '管理学院', '24物管A班', '香晖B806', '18820042368', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (53, '2024070301016', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '朱颖桐', 2, NULL, '440781200510248327', '2024070301016', '人文社会科学学院', '24新闻B班', '香晖C810', '13760512038', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (54, '2024080405001', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '蔡丹仪', 2, NULL, '445122200601064322', '2024080405001', '外国语学院', '24英语B班', '朝晖1130', '19566039235', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (55, '2024090101001', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '蔡嘉宜', 2, NULL, '441322200512111323', '2024090101001', '艺术设计学院', '24产设A班', '朝晖620', '13824711226', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (56, '2024050402004', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈灿群', 2, NULL, '445322200409293425', '2024050402004', '人文社会科学学院', '24新闻D班', '香晖A1121', '13178607019', '11', 1, '正式部员', '2026-07-12 12:57:07', '2026-07-18 23:43:43');
INSERT INTO `sys_user` VALUES (57, '2024090301003', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈李墨', 2, NULL, '350304200607251024', '2024090301003', '艺术设计学院', '24环设', '朝晖625', '13705946788', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (58, '2024050402010', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈颖茵', 2, NULL, '440781200607113840', '2024050402010', '人文社会科学学院', '24新闻D', '香晖A1121', '13676152524', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (59, '2024010202012', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '郭晓玉', 2, NULL, '469027200602100026', '2024010202012', '电信信息学院', '24电信B班', '香晖C118', '17389738393', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (60, '2024050402026', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '黄茜', 2, NULL, '441900200601205203', '2024050402026', '人文社会科学院', '24新闻D', '香晖A1124', '15899602937', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (61, '2024050402038', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '刘佳欣', 2, NULL, '441424200406016304', '2024050402038', '人文社会科学学院', '24新闻D班', '香晖A1121', '19128154601', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (62, '2024090301019', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '刘婉清', 2, NULL, '360921200511051221', '2024090301019', '艺术设计学院', '24环境设计A班', '朝晖620', '15180527186', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (63, '2024070102001', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '卢玥彤', 2, NULL, '440304200605154628', '2024070102001', '经贸学院', '24金融B班', '香晖C1022', '13622309023', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (64, '2024050402017', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '冯心悦', 2, NULL, '440781200605255327', '2024050402017', '人文社会科学学院', '24新闻D班', '香晖A1124', '13431730213', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (65, '2023090402041', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '许洁娜', 2, NULL, '445102200410146346', '2023090402041', '艺术设计学院', '23视传B', '凤翔1-508', '15919505330', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (66, '2024070101019', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '钟悦欣', 2, NULL, '440604200605180064', '2024070101019', '机电工程学院', '24电气B1', '香晖C902', '13612549937', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (67, '2023070204024', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈光翻', 1, NULL, '44122420050424705X', '2023070204024', '经贸学院', '23电商D', '17-749', '15119830104', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (68, '2024010201024', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈嘉豪', 1, NULL, '440881200510272437', '2024010201024', '电子信息学院', '24电信A班', '10-823', '13924214448', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (69, '2023090401005', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈梓鸿', 1, NULL, '440515200508201314', '2023090401005', '艺术设计学院', '23视传A', '8-217', '15767715658', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (70, '2024050401007', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '邓尧', 2, NULL, '441202200604290567', '2024050401007', '人文社会科学学院', '24新闻A', '香晖A812', '18998594931', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (71, '2024030801006', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '方烁森', 1, NULL, '445224200510121211', '2024030801006', '计算机学院', '24人工智能A班', '18-237', '15889653394', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (72, '2023090402011', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '郭华杰', 1, NULL, '350981200503090153', '2023090402011', '艺术设计学院', '23视传B', '八栋303', '15398885528', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (73, '2024020701018', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李昊阳', 1, NULL, '440181200510263911', '2024020701018', '机电工程学院', '24机电', '10-636', '17819775108', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (74, '2023080401003', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李佳蓉', 2, NULL, '610104200507286142', '2023080401003', '人文社会科学学院', '23新闻b班', '朝晖409', '18681529106', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (75, '2024060101034', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李晴', 1, NULL, '430626200603020032', '2024060101034', '管理学院', '24财管A班', '香晖B626', '18928877098', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (76, '2024030202094', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '吕淑言', 2, NULL, '350722200512220625', '2024030202094', '计算机学院', '24软工D班', '香晖B424', '13159299684', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (77, '2024050402046', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '欧紫盈', 2, NULL, '441781200502142742', '2024050402046', '人文社会科学学院', '24新闻D班', '香晖A1123', '19128001654', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (78, '2024090402028', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '石鹏程', 2, NULL, '360622200403210045', '2024090402028', '艺术设计学院', '24视传b班', '朝晖634', '13726326592', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (79, '2023050401078', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '王岱妍', 2, NULL, '440515200506290026', '2023050401078', '人文社会科学学院', '23新闻B', '凤翔3-402', '18316865007', NULL, 1, '留任部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (80, '2024020102069', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '文涛', 1, NULL, '445121200509277337', '2024020102069', '机电工程学院', '24电气B', '十栋541', '13827314124', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (81, '2024050402059', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '余全洁', 2, NULL, '411502200601112029', '2024050402059', '人文社会科学学院', '24新闻D', '香晖A1120', '18929516067', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (82, '2023050402038', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张伊漫', 2, NULL, '445122200409031723', '2023050402038', '人文社会科学学院', '23新闻C班', '3栋503', '15919848618', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (83, '2024090501049', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '赵凌梓', 1, NULL, '440105200608060913', '2024090501049', '艺术设计学院', '24工设a', '十栋302', '13544331456', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (84, '2024050402068', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '钟敏瑜', 2, NULL, '440305200512129067', '2024050402068', '人文社会科学学院', '24新闻C', '香晖A829', '13148529039', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (85, '2024010202072', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '钟文雄', 1, NULL, '350824200602140010', '2024010202072', '电子信息学院', '24 电信 b 班', '10 栋 434', '15728723302', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (86, '2024090101043', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '周楚盈', 2, NULL, '440605200512100164', '2024090101043', '艺术设计学院', '24产设A', '朝晖619', '13318397784', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (87, '2024050401071', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '朱榕翎', 2, NULL, '469007200409143360', '2024050401071', '人文社会科学学院', '24新闻A', '香晖A812', '13422126669', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (88, '2023050401072', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '魏黛琳', 2, '', '445202200406188020', '2023050401072', '人文社会科学学院', '23新闻B班', '3栋404', '13822097152', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:36:58');
INSERT INTO `sys_user` VALUES (89, '2023050401063', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '王滢', 2, '', '441900200507121329', '2023050401063', '人文社会科学学院', '23新闻B班', '凤翔3-409', '13532347206', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:36:10');
INSERT INTO `sys_user` VALUES (90, '2023060102006', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '龚楚淇', 2, '', '440103200410274841', '2023060102006', '管理学院', '23财务管理C班', '凤翔4栋205', '13680185067', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:36:43');
INSERT INTO `sys_user` VALUES (91, '2023080402027', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '何锌颖', 2, '', '44532220030214522X', '2023080402027', '外国语学院', '23英语B班', '朝晖523', '15818150965', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:37:09');
INSERT INTO `sys_user` VALUES (92, '2023050402060', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '梁衡', 1, '', '440803200503312913', '2023050402060', '人文社会科学学院', '23新闻D班', '8栋602', '13790971716', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:37:20');
INSERT INTO `sys_user` VALUES (93, '2023050402083', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张静文', 2, '', '440783200411150626', '2023050402083', '人文社会科学学院', '23新闻D班', '凤翔3栋606', '13066260393', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:37:33');
INSERT INTO `sys_user` VALUES (94, '2023050402053', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '杜美婷', 2, '', '411729200412257262', '2023050402053', '人文社会科学学院', '23新闻D班', '凤翔3栋608', '15814668266', '1', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:25:48');
INSERT INTO `sys_user` VALUES (95, '2023070103012', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '郑诗亭', 2, '', '445202200503063027', '2023070103012', '人文社会科学学院', '23法学a', '香晖b1205', '19840899982', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:38:25');
INSERT INTO `sys_user` VALUES (96, '2023050401056', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '钟利灵', 1, '', '441621200506260992', '2023050401056', '人文社会科学学院', '23新闻B班', '凤翔3-408', '18475684548', '', 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-22 09:38:55');
INSERT INTO `sys_user` VALUES (97, '2023050402064', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '林思慧', 0, NULL, NULL, '2023050402064', NULL, NULL, NULL, NULL, NULL, 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (98, '2023070103020', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '廖盈莹', 2, NULL, '441322200603111724', '2023070103020', '经贸学院', '23金融C', '香晖b1205', '15018096566', NULL, 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (99, '2023020301011', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '刘恒铭', 1, NULL, '411002200503271539', '2023020301011', '机电工程学院', '23自动化a1', NULL, '19876851267', NULL, 1, '主席团成员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (100, '2023050401029', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '蔡玉儿', 2, NULL, '440515200504210926', '2023050401029', '人文社会科学学院', '23新闻A', '凤翔3栋309', '13527192269', NULL, 1, '正式部员', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (101, '2025050601019', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '谭蘅萱', 2, NULL, '430225200606210163', '2025050601019', '人文社会科学学院', '25旅管A班', '香晖B321', '13149907621', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (102, '2025060301067', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李岚', 2, NULL, '440607200703140049', '2025060301067', '管理学院', '25人力资源管理B班', '香晖B1013', '13590929632', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (103, '2025050401011', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '甘莉', 2, NULL, '441622200602204669', '2025050401011', '人文社会科学学院', '25新闻B班', '香晖B104', '13510328321', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (104, '2025050402007', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈彤彤', 2, NULL, '441423200608290022', '2025050402007', '人文社会科学学院', '25新闻C班', '香晖B313', '13923023975', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (105, '2025050402004', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈黄敏', 2, NULL, '440513200608224027', '2025050402004', '人文社会科学学院', '25新闻B班', '香晖B313', '15875413615', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (106, '2025050402051', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '杨思涵', 2, NULL, '440512200707101229', '2025050402051', '人文社会科学学院', '25新闻C班', '香晖B316', '15218991715', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (107, '2025050402045', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '谢泳珠', 2, NULL, '440507200603032723', '2025050402045', '人文社会科学学院', '25新闻C班', '香晖B319', '13556393720', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (108, '2025050201034', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '许欣欣', 2, NULL, '440903200603020323', '2025050201034', '人文社会科学学院', '25行管A班', '香晖B210', '15875853640', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (109, '2024050402064', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张子萍', 2, NULL, '441481200608170024', '2024050402064', '人文社会科学学院', '24新闻C班', '香晖A828', '18320214527', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (110, '2025050402039', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陶晓君', 2, NULL, '441622200703050769', '2025050402039', '人文社会科学学院', '25新闻D班', '香晖B307', '13652528193', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (111, '2024050401023', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '黄雅韵', 2, NULL, '440902200510152427', '2024050401023', '人文社会科学学院', '24新闻A班', '香晖A814', '15338487053', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (112, '2025070304024', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '唐铭骏', 2, NULL, '350421200706017024', '2025070304024', '经贸学院', '25国贸D', '香晖c523', '13620090558', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (113, '2025050402059', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张晓莹', 2, NULL, '441882200610020023', '2025050402059', '人文社会科学学院', '25新闻D班', '香晖B318', '19820702178', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (114, '2025050601035', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈璐', 2, NULL, '340521200706152027', '2025050601035', '人文社会科学学院', '25旅管B班', '香晖B813', '19277583648', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (115, '2025050402020', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '康璐瑶', 2, NULL, '430503200611143020', '2025050402020', '人文社会科学学院', '25新闻c班', '香辉b307', '19865439792', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (116, '2025030205058', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '徐旸凯', 1, NULL, '441224200606115410', '2025030205058', '计算机学院', '25软工F班', '12栋306', '13432445961', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (117, '2025050402016', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '黄立欣', 2, NULL, '442000200705250887', '2025050402016', '人文社会科学学院', '25新闻D班', '香晖B314', '18924983430', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (118, '2024040801034', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张子豪', 1, NULL, '441424200511260536', '2024040801034', '电子信息学院', '24通信A班', '19栋607', '13826454513', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (119, '2025030203021', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '黎林颖', 2, NULL, '440981200610174709', '2025030203021', '计算机学院', '25软工A班', '香晖A309', '19876967881', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (120, '2025020101064', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '刘宇敏', 2, NULL, '440203200610091826', '2025020101064', '机电工程学院', '25电气A2班', '朝晖319', '15361330263', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (121, '2025010401013', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '何志杰', 1, NULL, '360481200705092637', '2025010401013', '电子信息学院', '25光电', '13栋322', '18676962637', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (122, '2025050102064', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '林子珊', 2, NULL, '440105200708157421', '2025050102064', '人文社会科学学院', '25法学D班', '香晖B126', '13662509898', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (123, '2025050401053', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张梦', 2, NULL, '440305200611057847', '2025050401053', '人文社会科学学院', '25新闻B班', '香晖B305', '19868278020', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (124, '2025080301016', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李沛家', 2, NULL, '440604200709130125', '2025080301016', '外国语学院', '25商英A班', '朝晖205', '15015568363', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (125, '2025050102049', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '管梓莹', 2, NULL, '441422200707304227', '2025050102049', '人文社会科学学院', '25法学D班', '香晖B206', '18676991538', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (126, '2025050401039', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '彭可昕', 2, NULL, '441523200703227609', '2025050401039', '人文社会科学学院', '25新闻A', '香晖B302', '19306604698', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (127, '2025050201017', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '李满儿', 2, NULL, '445121200702012648', '2025050201017', '人文社会科学学院', '25行政管理A班', '香晖B208', '13534620667', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (128, '2025040703058', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '何洁', 2, NULL, '440604200708150167', '2025040703058', '材料与食品学院', '25食品质量与安全B班', '香晖A423', '13827722788', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (129, '2025050402043', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '肖可欣', 2, NULL, '445102200711051228', '2025050402043', '人文社会科学学院', '25新闻D班', '香晖B319', '19886485105', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (130, '2025040704055', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈之茵', 2, NULL, '445281200502142482', '2025040704055', '材料与食品学院', '25食安D班', '香晖A912', '13113448076', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (131, '2025040703022', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '袁咏仪', 2, NULL, '431021200609180021', '2025040703022', '材料与食品学院', '25食安A班', '香晖A407', '18575873985', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (132, '2025050201079', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '周胡韵瑾', 2, NULL, '43102120070127002X', '2025050201079', '人文社会科学学院', '25行政管理b班', '香晖b214', '18627351875', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (133, '2025050402042', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '吴宇萱', 2, NULL, '440304200610250040', '2025050402042', '人文社会科学学院', '25新闻d班', '香晖b315', '18038059732', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (134, '2025090401005', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈乐琪', 2, NULL, '440903200711241609', '2025090401005', '艺术设计学院', '25视觉传达设计A班', '11栋202', '18027770267', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (135, '2025090401049', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '张可盈', 2, NULL, '441423200702041444', '2025090401049', '艺术设计学院', '25视传A班', '11栋208', '18928461895', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (136, '2025090401026', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '刘境宜', 2, NULL, '445221200610187825', '2025090401026', '艺术设计学院', '25视觉传达设计A班', '11栋202', '19075104060', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (137, '2025050402030', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '林梓萌', 2, NULL, '445122200608045626', '2025050402030', '人文社会科学学院', '25新闻D班', '香晖B319', '13413747233', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (138, '2025090501037', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '马雯', 2, NULL, '440403200704229129', '2025090501037', '艺术设计学院', '25工业设计', '香晖C611', '13005524506', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (139, '2025010301058', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '温玉锴', 1, NULL, '440233200603083017', '2025010301058', '电子信息学院', '25通信A班', '13栋111', '17875548307', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (140, '2025040802037', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '刘春霞', 2, NULL, '441781200704095921', '2025040802037', '材料与食品学院', '25应用化学D', '香晖A528', '15118013217', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (141, '2025050402019', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '金思彤', 2, NULL, '330382200705261723', '2025050402019', '人文社会科学学院', '25新闻C班', '香晖B318', '18807607674', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (142, '2025050402014', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '黄慧淳', 2, NULL, '445102200706131223', '2025050402014', '人文社会科学学院', '25新闻C班', '11-320', '15816522986', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (143, '2025070201029', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '余牧遥', 2, NULL, '44051520070414162X', '2025070201029', '经贸学院', '25电子商务A班', '香晖C412', '13539675653', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (144, '2025050401050', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '叶雪茵', 2, NULL, '440115200611040924', '2025050401050', '人文社会科学学院', '25新闻A班', '香晖B228', '15986308751', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (145, '2025050601005', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '曾子琪', 2, NULL, '440883200507291963', '2025050601005', '人文社会科学学院', '25旅管A班', '香晖B324', '15107601071', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (146, '2025020102069', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '王智旭', 1, NULL, '440305200701060417', '2025020102069', '机电工程学院', '25电气B2', '15栋303', '18952438257', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (147, '2025070303025', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '邓婕', 2, NULL, '431021200707108648', '2025070303025', '经贸学院', '25国贸C班', '香晖C515', '15675661332', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (148, '2024050104061', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '覃彤彬', 2, NULL, '430523200708146629', '2024050104061', '人文社会科学学院', '24法学F班', '香晖A720', '19925469068', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (149, '2025060301003', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '陈羽悦', 2, NULL, '360721200704127623', '2025060301003', '管理学院', '25人力资源管理A班', '香晖B1006', '13798778713', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (150, '2025010301035', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '廖宏泽', 1, NULL, '441284200705100050', '2025010301035', '电子信息学院', '25通信A班', '13栋103', '18078822982', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (151, '2025020701043', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '郭安', 1, NULL, '14062120070326403X', '2025020701043', '机电工程学院', '25机电B', '15栋607', '13590998057', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (152, '2025070302001', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '苏汉文', 1, NULL, '350212200704070019', '2025070302001', '经贸学院', '25国际经济与贸易B班', '13栋610', '19890395285', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (153, '2025050401013', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '关雅文', 2, NULL, '440605200706270022', '2025050401013', '人文社会科学学院', '25新闻A班', '香晖B302', '18022228627', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (154, '2025050402031', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '林曦晴', 2, NULL, '440114200709290220', '2025050402031', '人文社会科学学院', '25新闻D', '香晖B309', '13422167579', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');
INSERT INTO `sys_user` VALUES (155, '2025050102073', '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm', '王锴悦', 2, NULL, '360321200706154527', '2025050102073', '人文社会科学学院', '25法学D班', '香晖苑b126', '13827276909', NULL, 1, '实习部员（二轮）', '2026-07-12 12:57:07', '2026-07-12 12:57:07');

-- ----------------------------
-- Table structure for sys_user_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_dept`;
CREATE TABLE `sys_user_dept`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NOT NULL,
  `dept_id` bigint UNSIGNED NOT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_dept`(`user_id` ASC, `dept_id` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 277 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_dept
-- ----------------------------
INSERT INTO `sys_user_dept` VALUES (1, 3, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (2, 4, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (3, 5, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (4, 6, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (5, 7, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (6, 8, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (7, 9, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (8, 10, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (9, 11, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (10, 12, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (11, 13, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (12, 14, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (13, 15, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (14, 16, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (15, 17, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (16, 18, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (17, 19, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (18, 20, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (19, 21, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (20, 22, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (21, 23, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (22, 24, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (23, 25, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (24, 101, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (25, 102, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (26, 103, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (27, 104, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (28, 105, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (29, 106, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (30, 107, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (31, 108, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (32, 109, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (33, 110, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (34, 111, 2, '采编部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (35, 26, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (36, 27, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (37, 28, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (38, 29, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (39, 30, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (40, 31, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (41, 32, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (42, 33, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (43, 34, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (44, 35, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (45, 36, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (46, 37, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (47, 38, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (48, 39, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (49, 40, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (50, 41, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (51, 42, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (52, 43, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (53, 44, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (54, 45, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (55, 46, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (56, 47, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (57, 48, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (58, 49, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (59, 50, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (60, 51, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (61, 52, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (62, 53, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (63, 112, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (64, 113, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (65, 114, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (66, 115, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (67, 116, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (68, 117, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (69, 118, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (70, 119, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (71, 120, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (72, 121, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (73, 122, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (74, 123, 3, '摄影部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (85, 97, 1, '主席团', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (86, 98, 1, '主席团', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (87, 99, 1, '主席团', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (88, 67, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (89, 68, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (90, 69, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (91, 70, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (92, 71, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (93, 72, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (94, 73, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (95, 74, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (96, 75, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (97, 76, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (98, 77, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (99, 78, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (100, 79, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (101, 80, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (102, 81, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (103, 82, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (104, 83, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (105, 84, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (106, 85, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (107, 86, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (108, 87, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (109, 100, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (110, 139, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (111, 140, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (112, 141, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (113, 142, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (114, 143, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (115, 144, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (116, 145, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (117, 146, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (118, 147, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (119, 148, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (120, 149, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (121, 150, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (122, 151, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (123, 152, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (124, 153, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (125, 154, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (126, 155, 5, '视频部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (127, 54, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (128, 55, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (129, 56, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (130, 57, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (131, 58, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (132, 59, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (133, 60, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (134, 61, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (135, 62, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (136, 63, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (137, 64, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (138, 65, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (139, 66, 4, '视觉部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (140, 124, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (141, 125, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (142, 126, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (143, 127, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (144, 128, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (145, 129, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (146, 130, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (147, 131, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (148, 132, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (149, 133, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (150, 134, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (151, 135, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (152, 136, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (153, 137, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (154, 138, 4, '视觉传达与技术部', '2026-07-12 12:57:07');
INSERT INTO `sys_user_dept` VALUES (256, 2, 1, NULL, '2026-07-12 12:57:58');
INSERT INTO `sys_user_dept` VALUES (257, 2, 4, NULL, '2026-07-12 12:57:58');
INSERT INTO `sys_user_dept` VALUES (258, 1, 1, NULL, '2026-07-12 15:22:22');
INSERT INTO `sys_user_dept` VALUES (259, 89, 1, NULL, '2026-07-22 09:36:10');
INSERT INTO `sys_user_dept` VALUES (260, 89, 4, NULL, '2026-07-22 09:36:10');
INSERT INTO `sys_user_dept` VALUES (261, 90, 1, NULL, '2026-07-22 09:36:43');
INSERT INTO `sys_user_dept` VALUES (262, 90, 2, NULL, '2026-07-22 09:36:43');
INSERT INTO `sys_user_dept` VALUES (263, 88, 1, NULL, '2026-07-22 09:36:58');
INSERT INTO `sys_user_dept` VALUES (264, 88, 2, NULL, '2026-07-22 09:36:58');
INSERT INTO `sys_user_dept` VALUES (265, 91, 1, NULL, '2026-07-22 09:37:09');
INSERT INTO `sys_user_dept` VALUES (266, 91, 3, NULL, '2026-07-22 09:37:09');
INSERT INTO `sys_user_dept` VALUES (267, 92, 1, NULL, '2026-07-22 09:37:20');
INSERT INTO `sys_user_dept` VALUES (268, 92, 2, NULL, '2026-07-22 09:37:20');
INSERT INTO `sys_user_dept` VALUES (269, 93, 1, NULL, '2026-07-22 09:37:33');
INSERT INTO `sys_user_dept` VALUES (270, 93, 4, NULL, '2026-07-22 09:37:33');
INSERT INTO `sys_user_dept` VALUES (271, 94, 1, NULL, '2026-07-22 09:38:08');
INSERT INTO `sys_user_dept` VALUES (272, 94, 2, NULL, '2026-07-22 09:38:08');
INSERT INTO `sys_user_dept` VALUES (273, 95, 1, NULL, '2026-07-22 09:38:25');
INSERT INTO `sys_user_dept` VALUES (274, 95, 2, NULL, '2026-07-22 09:38:25');
INSERT INTO `sys_user_dept` VALUES (275, 96, 1, NULL, '2026-07-22 09:38:55');
INSERT INTO `sys_user_dept` VALUES (276, 96, 2, NULL, '2026-07-22 09:38:55');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NOT NULL,
  `role_id` bigint UNSIGNED NOT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 271 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (3, 3, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (4, 4, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (5, 5, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (6, 6, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (7, 7, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (8, 8, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (9, 9, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (10, 10, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (11, 11, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (12, 12, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (13, 13, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (14, 14, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (15, 15, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (16, 16, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (17, 17, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (18, 18, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (19, 19, 15, '正式成员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (20, 20, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (21, 21, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (22, 22, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (23, 23, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (24, 24, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (25, 25, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (26, 26, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (27, 27, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (28, 28, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (29, 29, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (30, 30, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (31, 31, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (32, 32, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (33, 33, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (34, 34, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (35, 35, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (36, 36, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (37, 37, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (38, 38, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (39, 39, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (40, 40, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (41, 41, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (42, 42, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (43, 43, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (44, 44, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (45, 45, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (46, 46, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (47, 47, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (48, 48, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (49, 49, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (50, 50, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (51, 51, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (52, 52, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (53, 53, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (54, 54, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (55, 55, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (56, 56, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (57, 57, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (58, 58, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (59, 59, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (60, 60, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (61, 61, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (62, 62, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (63, 63, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (64, 64, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (65, 65, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (66, 66, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (67, 67, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (68, 68, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (69, 69, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (70, 70, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (71, 71, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (72, 72, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (73, 73, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (74, 74, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (75, 75, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (76, 76, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (77, 77, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (78, 78, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (79, 79, 10, '留任部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (80, 80, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (81, 81, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (82, 82, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (83, 83, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (84, 84, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (85, 85, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (86, 86, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (87, 87, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (97, 97, 1, '主席团成员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (98, 98, 1, '主席团成员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (99, 99, 1, '主席团成员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (100, 100, 15, '正式部员', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (101, 101, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (102, 102, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (103, 103, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (104, 104, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (105, 105, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (106, 106, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (107, 107, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (108, 108, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (109, 109, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (110, 110, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (111, 111, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (112, 112, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (113, 113, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (114, 114, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (115, 115, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (116, 116, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (117, 117, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (118, 118, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (119, 119, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (120, 120, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (121, 121, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (122, 122, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (123, 123, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (124, 124, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (125, 125, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (126, 126, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (127, 127, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (128, 128, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (129, 129, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (130, 130, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (131, 131, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (132, 132, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (133, 133, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (134, 134, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (135, 135, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (136, 136, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (137, 137, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (138, 138, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (139, 139, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (140, 140, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (141, 141, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (142, 142, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (143, 143, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (144, 144, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (145, 145, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (146, 146, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (147, 147, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (148, 148, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (149, 149, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (150, 150, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (151, 151, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (152, 152, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (153, 153, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (154, 154, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (155, 155, 9, '实习部员（二轮）', '2026-07-12 12:57:07');
INSERT INTO `sys_user_role` VALUES (256, 2, 3, NULL, '2026-07-12 12:57:58');
INSERT INTO `sys_user_role` VALUES (257, 1, 14, NULL, '2026-07-12 15:22:22');
INSERT INTO `sys_user_role` VALUES (258, 89, 4, NULL, '2026-07-22 09:36:10');
INSERT INTO `sys_user_role` VALUES (259, 89, 6, NULL, '2026-07-22 09:36:10');
INSERT INTO `sys_user_role` VALUES (260, 90, 4, NULL, '2026-07-22 09:36:43');
INSERT INTO `sys_user_role` VALUES (261, 90, 5, NULL, '2026-07-22 09:36:43');
INSERT INTO `sys_user_role` VALUES (262, 88, 1, NULL, '2026-07-22 09:36:58');
INSERT INTO `sys_user_role` VALUES (263, 91, 4, NULL, '2026-07-22 09:37:09');
INSERT INTO `sys_user_role` VALUES (264, 92, 4, NULL, '2026-07-22 09:37:20');
INSERT INTO `sys_user_role` VALUES (265, 93, 4, NULL, '2026-07-22 09:37:33');
INSERT INTO `sys_user_role` VALUES (266, 93, 6, NULL, '2026-07-22 09:37:33');
INSERT INTO `sys_user_role` VALUES (267, 94, 4, NULL, '2026-07-22 09:38:08');
INSERT INTO `sys_user_role` VALUES (268, 95, 4, NULL, '2026-07-22 09:38:25');
INSERT INTO `sys_user_role` VALUES (269, 96, 4, NULL, '2026-07-22 09:38:55');
INSERT INTO `sys_user_role` VALUES (270, 96, 5, NULL, '2026-07-22 09:38:55');

SET FOREIGN_KEY_CHECKS = 1;
