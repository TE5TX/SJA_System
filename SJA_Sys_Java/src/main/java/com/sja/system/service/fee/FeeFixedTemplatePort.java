package com.sja.system.service.fee;

import com.sja.system.application.vo.fee.FeeDetailVO;
import com.sja.system.application.vo.fee.FeeListVO;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 固定稿费模板写入端口，隔离导出用例与具体 Excel 实现。
 */
// 202609210900 - 修复了稿费导出用例直接依赖service.impl模板组件的问题。
public interface FeeFixedTemplatePort {

    boolean supports(String templateType);

    void write(OutputStream output, String templateType, Iterable<ExportItem> items,
               String selectedMonth) throws IOException;

    record ExportItem(FeeListVO item, FeeDetailVO detail) {
    }
}
