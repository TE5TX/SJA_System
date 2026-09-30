package com.sja.system.application.model;

import com.sja.system.common.ApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** 小型导入文件的不可变快照；预览解析与原件归档读取完全相同的字节。 */
// 202609291708 - 修复了人员预览和原件归档二次读取可能内容不一致的问题。
public final class BufferedUploadedFile implements UploadedFile {

    private final String originalFilename;
    private final byte[] bytes;

    private BufferedUploadedFile(String originalFilename, byte[] bytes) {
        this.originalFilename = originalFilename;
        this.bytes = bytes.clone();
    }

    public static BufferedUploadedFile capture(UploadedFile source, long maxBytes) {
        if (source == null || source.empty()) throw ApiException.badRequest("请选择要导入的xlsx文件");
        if (source.size() > maxBytes) throw ApiException.badRequest("xlsx文件不能超过2MB");
        try (InputStream stream = source.inputStream()) {
            byte[] content = stream.readNBytes((int) maxBytes + 1);
            if (content.length > maxBytes) throw ApiException.badRequest("xlsx文件不能超过2MB");
            return new BufferedUploadedFile(source.originalFilename(), content);
        } catch (IOException e) {
            throw ApiException.badRequest("无法读取上传的Excel文件");
        }
    }

    @Override public String originalFilename() { return originalFilename; }
    @Override public long size() { return bytes.length; }
    @Override public boolean empty() { return bytes.length == 0; }
    @Override public InputStream inputStream() { return new ByteArrayInputStream(bytes); }
}
