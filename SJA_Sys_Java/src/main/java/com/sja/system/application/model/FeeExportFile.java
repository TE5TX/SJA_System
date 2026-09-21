package com.sja.system.application.model;

import java.util.Objects;

/**
 * 与 HTTP 传输协议无关的稿费导出文件。
 *
 * @param fileName   未编码的下载文件名
 * @param contentType 文件内容类型
 * @param content    文件二进制内容
 */
// 202609201130 - 完成了稿费导出结果与Servlet响应对象的解耦。
public record FeeExportFile(String fileName, String contentType, byte[] content) implements DownloadableFile {

    public static final String XLSX_CONTENT_TYPE = DownloadableFile.XLSX_CONTENT_TYPE;

    public FeeExportFile {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(contentType, "contentType");
        content = Objects.requireNonNull(content, "content").clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
