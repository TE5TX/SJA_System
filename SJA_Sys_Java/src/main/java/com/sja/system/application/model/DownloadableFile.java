package com.sja.system.application.model;

/**
 * 与 HTTP 传输协议无关的可下载文件模型。
 */
// 202609201621 - 完成了稿费、人员与素拓导出文件模型的统一抽象。
public interface DownloadableFile {

    String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    String fileName();

    String contentType();

    byte[] content();
}
