-- 202609291708 - 完成了文件存储目录设置和原始XLSX归档的数据结构升级。
-- 在现有数据库上执行一次；目录只保存相对路径，实际根目录由运行环境提供。
CREATE TABLE IF NOT EXISTS `sys_storage_location` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `category` VARCHAR(16) NOT NULL COMMENT 'AVATAR或IMPORT',
  `relative_dir` VARCHAR(200) NOT NULL COMMENT '存储根目录下的相对子目录',
  `active` TINYINT(1) NOT NULL DEFAULT 0,
  `created_by` BIGINT UNSIGNED NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_storage_category_active` (`category`, `active`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件存储位置历史';

INSERT INTO `sys_storage_location` (`category`, `relative_dir`, `active`)
SELECT 'AVATAR', 'avatars', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_storage_location` WHERE `category` = 'AVATAR');
INSERT INTO `sys_storage_location` (`category`, `relative_dir`, `active`)
SELECT 'IMPORT', 'imports', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_storage_location` WHERE `category` = 'IMPORT');

CREATE TABLE IF NOT EXISTS `sys_import_archive` (
  `id` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `storage_location_id` BIGINT UNSIGNED NOT NULL,
  `stored_name` VARCHAR(64) NOT NULL COMMENT '年份/月/UUID.xlsx',
  `original_name` VARCHAR(200) NOT NULL,
  `sha256` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `file_size` BIGINT UNSIGNED NOT NULL,
  `import_type` VARCHAR(16) NOT NULL COMMENT 'BASIC或ROSTER',
  `operator_id` BIGINT UNSIGNED NOT NULL,
  `operator_name` VARCHAR(100) NOT NULL,
  `token_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `status` VARCHAR(20) NOT NULL COMMENT 'PREVIEWED/SUCCESS/PARTIAL/ABORTED',
  `total_count` INT NOT NULL DEFAULT 0,
  `imported_count` INT NOT NULL DEFAULT 0,
  `skipped_count` INT NOT NULL DEFAULT 0,
  `unprocessed_count` INT NOT NULL DEFAULT 0,
  `result_message` VARCHAR(500) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirmed_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_import_archive_token` (`token_hash`),
  KEY `idx_import_archive_time` (`create_time`, `id`),
  KEY `idx_import_archive_operator` (`operator_id`, `create_time`),
  CONSTRAINT `fk_import_archive_location` FOREIGN KEY (`storage_location_id`)
    REFERENCES `sys_storage_location` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员导入原始文件及结果';
