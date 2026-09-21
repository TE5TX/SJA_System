package com.sja.system.application.model;

import java.util.Objects;

/**
 * 与 HTTP 传输协议无关的素拓导出文件。
 *
 * @param fileName    未编码的下载文件名
 * @param contentType 文件内容类型
 * @param content     文件二进制内容
 */
// 202609201621 - 完成了素拓导出文件对统一下载模型的适配。
public record SujiaExportFile(String fileName, String contentType, byte[] content) implements DownloadableFile {

    public SujiaExportFile {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(contentType, "contentType");
        content = Objects.requireNonNull(content, "content").clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
