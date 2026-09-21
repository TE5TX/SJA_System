package com.sja.system.framework.web.support;

import com.sja.system.application.model.DownloadableFile;
import com.sja.system.application.model.UserExportFile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadResponseFactoryTest {

    @Test
    void writesAsciiFallbackAndRfc5987ChineseFileName() {
        byte[] content = {1, 2, 3};
        UserExportFile file = new UserExportFile("校学生记者团稿费统计（微信）.xlsx",
                DownloadableFile.XLSX_CONTENT_TYPE, content);

        ResponseEntity<byte[]> response = DownloadResponseFactory.create(file);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        // 202609201621 - 验证所有中文下载名统一使用RFC 5987 UTF-8编码。
        assertTrue(disposition.startsWith("attachment; filename=\"download.xlsx\"; filename*=UTF-8''"));
        assertTrue(disposition.contains("%E6%A0%A1%E5%AD%A6%E7%94%9F"));
        assertTrue(disposition.contains("%EF%BC%88%E5%BE%AE%E4%BF%A1%EF%BC%89.xlsx"));
        assertEquals(content.length, response.getHeaders().getContentLength());
        assertArrayEquals(content, response.getBody());
    }

    @Test
    void encodesSpacesAsPercent20InsteadOfPlus() {
        UserExportFile file = new UserExportFile("fee export 2026.xlsx",
                DownloadableFile.XLSX_CONTENT_TYPE, new byte[0]);

        String disposition = DownloadResponseFactory.create(file)
                .getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);

        assertTrue(disposition.contains("filename=\"fee-export-2026.xlsx\""));
        assertTrue(disposition.contains("filename*=UTF-8''fee%20export%202026.xlsx"));
        assertFalse(disposition.contains("+"));
    }

    @Test
    void rejectsResponseHeaderInjectionCharacters() {
        UserExportFile file = new UserExportFile("report.xlsx\r\nX-Test: injected",
                DownloadableFile.XLSX_CONTENT_TYPE, new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> DownloadResponseFactory.create(file));
    }

    @Test
    void rejectsPathCharactersInFileName() {
        UserExportFile file = new UserExportFile("../report.xlsx",
                DownloadableFile.XLSX_CONTENT_TYPE, new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> DownloadResponseFactory.create(file));
    }
}
