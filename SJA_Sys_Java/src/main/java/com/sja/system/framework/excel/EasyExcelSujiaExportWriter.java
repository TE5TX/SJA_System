package com.sja.system.framework.excel;

import com.alibaba.excel.EasyExcel;
import com.sja.system.application.port.SujiaExportPort;
import com.sja.system.application.vo.tool.SujiaUserVO;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * EasyExcel adapter for the Sujia workbook format.
 */
@Component
public class EasyExcelSujiaExportWriter implements SujiaExportPort {

    @Override
    public byte[] write(List<SujiaUserVO> rows) {
        List<List<String>> head = Arrays.asList(
                Collections.singletonList("姓名"),
                Collections.singletonList("学号"));
        List<List<String>> data = rows.stream()
                .map(row -> Arrays.asList(
                        row.getRealName(),
                        row.getStudentNo() == null ? "" : row.getStudentNo()))
                .toList();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output).head(head).sheet("素拓名单").doWrite(data);
        return output.toByteArray();
    }
}
