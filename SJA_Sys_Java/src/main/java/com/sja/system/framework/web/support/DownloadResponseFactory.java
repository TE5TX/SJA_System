package com.sja.system.framework.web.support;

import com.sja.system.application.model.DownloadableFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * 将应用层文件模型转换为标准 HTTP 下载响应。
 *
 * <p>文件名同时提供 ASCII 回退值和 RFC 5987 UTF-8 扩展值，兼容旧客户端与中文文件名。</p>
 */
public final class DownloadResponseFactory {

    private static final int MAX_FILE_NAME_LENGTH = 180;
    private static final int MAX_FALLBACK_BASE_LENGTH = 80;
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private DownloadResponseFactory() {
    }

    /**
     * 创建包含标准文件名响应头、内容类型和内容长度的下载响应。
     */
    public static ResponseEntity<byte[]> create(DownloadableFile file) {
        Objects.requireNonNull(file, "下载文件不能为空");
        String safeFileName = validateFileName(file.fileName());
        byte[] content = Objects.requireNonNull(file.content(), "下载文件内容不能为空");
        String disposition = "attachment; filename=\"" + asciiFallback(safeFileName)
                + "\"; filename*=UTF-8''" + encodeRfc5987(safeFileName);
        // 202609201621 - 统一使用RFC 5987 filename*输出中文下载文件名。
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(content);
    }

    private static String validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("下载文件名不能为空");
        }
        String normalized = fileName.trim();
        if (normalized.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("下载文件名不能超过" + MAX_FILE_NAME_LENGTH + "个字符");
        }
        if (normalized.chars().anyMatch(value -> value < 32 || value == 127)
                || normalized.indexOf('/') >= 0 || normalized.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("下载文件名包含非法字符");
        }
        return normalized;
    }

    private static String asciiFallback(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        String extension = extensionIndex > 0
                ? fileName.substring(extensionIndex).replaceAll("[^A-Za-z0-9.]", "")
                : "";
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        String asciiBase = baseName.replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[._-]+|[._-]+$", "");
        if (asciiBase.isBlank()) {
            asciiBase = "download";
        }
        if (asciiBase.length() > MAX_FALLBACK_BASE_LENGTH) {
            asciiBase = asciiBase.substring(0, MAX_FALLBACK_BASE_LENGTH);
        }
        return asciiBase + extension.toLowerCase(Locale.ROOT);
    }

    private static String encodeRfc5987(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length * 3);
        for (byte current : bytes) {
            int unsigned = current & 0xFF;
            if (isAttributeCharacter(unsigned)) {
                encoded.append((char) unsigned);
            } else {
                encoded.append('%')
                        .append(HEX[(unsigned >>> 4) & 0x0F])
                        .append(HEX[unsigned & 0x0F]);
            }
        }
        return encoded.toString();
    }

    private static boolean isAttributeCharacter(int value) {
        return value >= 'a' && value <= 'z'
                || value >= 'A' && value <= 'Z'
                || value >= '0' && value <= '9'
                || value == '!' || value == '#' || value == '$' || value == '&'
                || value == '+' || value == '-' || value == '.' || value == '^'
                || value == '_' || value == '`' || value == '|' || value == '~';
    }
}
