package com.sja.system.application.model;

import java.nio.file.Path;

/**
 * 已校验且允许读取的头像文件。
 */
// 202609211059 - 完成了头像读取结果与HTTP响应对象的解耦。
public record AvatarFile(Path path, String contentType) {
}
