package com.sja.system.application.vo.user;

import java.time.LocalDateTime;

/** 不包含服务端路径及确认令牌的导入原件归档摘要。 */
// 202609291708 - 完成了人员导入归档对前端的最小信息展示。
public record ImportArchiveVO(String id, String originalName, String sha256, long fileSize,
                              String importType, String operatorName, String status,
                              int totalCount, int importedCount, int skippedCount,
                              int unprocessedCount, String message,
                              LocalDateTime createdAt, LocalDateTime confirmedAt) { }
