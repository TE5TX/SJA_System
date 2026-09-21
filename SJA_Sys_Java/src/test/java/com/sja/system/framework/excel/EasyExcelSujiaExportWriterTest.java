package com.sja.system.framework.excel;

import com.sja.system.application.vo.tool.SujiaUserVO;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EasyExcelSujiaExportWriterTest {

    @Test
    void writesTheExpectedHeadersAndRows() throws Exception {
        byte[] content = new EasyExcelSujiaExportWriter().write(
                List.of(new SujiaUserVO("张三", "20260001", true)));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertEquals("姓名", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("学号", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("张三", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("20260001", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
        }
    }
}
