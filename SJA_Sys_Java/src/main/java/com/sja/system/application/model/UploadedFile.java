package com.sja.system.application.model;

import java.io.IOException;
import java.io.InputStream;

/**
 * 传输无关的上传文件接口。
 *
 * <p>业务服务只依赖文件名、大小和内容流，不感知 Spring Web 的 MultipartFile。</p>
 */
// 202609211059 - 完成了上传文件输入模型与Spring Web类型的解耦。
public interface UploadedFile {

    String originalFilename();

    long size();

    boolean empty();

    InputStream inputStream() throws IOException;
}
