package com.sja.system.service.impl;

import com.sja.system.service.fee.FeeFixedTemplatePort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * 使用现有固定工作簿写入器实现应用层模板端口。
 */
@Component
// 202609210900 - 完成了固定稿费模板Excel实现对应用层端口的适配。
public class FeeFixedTemplateAdapter implements FeeFixedTemplatePort {

    @Override
    public boolean supports(String templateType) {
        return FeeTemplateExportWriter.supports(templateType);
    }

    @Override
    public void write(OutputStream output, String templateType, Iterable<ExportItem> items,
                      String selectedMonth) throws IOException {
        FeeTemplateExportWriter.write(output, templateType, adapt(items), selectedMonth);
    }

    private Iterable<FeeTemplateExportWriter.ExportItem> adapt(Iterable<ExportItem> source) {
        return () -> new Iterator<>() {
            private final Iterator<ExportItem> delegate = source.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public FeeTemplateExportWriter.ExportItem next() {
                ExportItem item = delegate.next();
                return new FeeTemplateExportWriter.ExportItem(item.item(), item.detail());
            }
        };
    }
}
