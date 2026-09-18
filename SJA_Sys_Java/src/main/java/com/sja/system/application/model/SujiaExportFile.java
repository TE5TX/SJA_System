package com.sja.system.application.model;

import java.util.Objects;

/**
 * Transport-neutral downloadable file produced by the Sujia use case.
 */
public record SujiaExportFile(String fileName, String contentType, byte[] content) {

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
