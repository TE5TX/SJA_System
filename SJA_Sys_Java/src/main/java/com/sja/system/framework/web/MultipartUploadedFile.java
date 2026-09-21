package com.sja.system.framework.web;

import com.sja.system.application.model.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Spring MultipartFile 到应用层上传文件接口的Web适配器。
 */
// 202609211059 - 完成了MultipartFile仅保留在Web基础设施层的任务。
public final class MultipartUploadedFile implements UploadedFile {

    private final MultipartFile source;

    public MultipartUploadedFile(MultipartFile source) {
        this.source = Objects.requireNonNull(source, "上传文件不能为空");
    }

    @Override
    public String originalFilename() {
        return source.getOriginalFilename();
    }

    @Override
    public long size() {
        return source.getSize();
    }

    @Override
    public boolean empty() {
        return source.isEmpty();
    }

    @Override
    public InputStream inputStream() throws IOException {
        return source.getInputStream();
    }
}
