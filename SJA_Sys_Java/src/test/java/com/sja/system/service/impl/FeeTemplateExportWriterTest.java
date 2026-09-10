package com.sja.system.service.impl;

import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FeeTemplateExportWriterTest {

    @Test
    void writesWechatWorkbookUsingExistingDepartmentTemplate() throws Exception {
        FeeListVO item = item("EDITORIAL", "微信", "测试推文");
        FeeDetailVO detail = detail(new LinkedHashMap<String, Object>() {{
            put("category", "原创类");
            put("publishDate", LocalDate.of(2026, 6, 18));
            put("articleLink", "https://example.com/wechat");
            put("readCount", 6800);
        }}, contributor("张三", "AUTHOR", "50.0", null), contributor("李四", "AUTHOR", "50.0", null));

        Workbook workbook = writeAndRead(FeeTemplateExportWriter.EDITORIAL_WECHAT, item, detail);
        Sheet sheet = workbook.getSheetAt(0);
        assertEquals("校学生记者团稿费统计（微信）", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("月份", sheet.getRow(2).getCell(0).getStringCellValue());
        assertEquals("阅读量", sheet.getRow(2).getCell(6).getStringCellValue());
        assertEquals("6月", sheet.getRow(3).getCell(0).getStringCellValue());
        assertEquals("原创类稿件", sheet.getRow(3).getCell(1).getStringCellValue());
        assertEquals("张三 李四", sheet.getRow(3).getCell(4).getStringCellValue());
        assertEquals(50d, sheet.getRow(3).getCell(8).getNumericCellValue());
        assertEquals(2d, sheet.getRow(3).getCell(9).getNumericCellValue());
        assertEquals(2, sheet.getNumMergedRegions());
        assertEquals(new CellRangeAddress(0, 0, 0, 11).formatAsString(), sheet.getMergedRegion(0).formatAsString());
        assertEquals(HorizontalAlignment.CENTER, sheet.getRow(3).getCell(2).getCellStyle().getAlignment());
        assertEquals(BorderStyle.THIN, sheet.getRow(3).getCell(2).getCellStyle().getBorderBottom());
        workbook.close();
    }

    @Test
    void writesVideoShootersAndEditorsIntoSeparateTemplateColumns() throws Exception {
        FeeListVO item = item("VIDEO", null, "毕业季视频");
        FeeDetailVO detail = detail(new LinkedHashMap<String, Object>() {{
            put("category", "活动新闻类视频");
            put("publishDate", LocalDate.of(2026, 6, 23));
            put("videoLink", "https://example.com/video");
            put("isReposted", 1);
            put("isSpecialNode", 1);
        }}, contributor("甲", "SHOOTER", "350.0", null), contributor("乙", "SHOOTER", "350.0", null),
                contributor("丙", "EDITOR", "300.0", null));

        Workbook workbook = writeAndRead(FeeTemplateExportWriter.VIDEO, item, detail);
        Row row = workbook.getSheetAt(0).getRow(3);
        assertEquals("活动新闻类视频+特殊节点视频", row.getCell(1).getStringCellValue());
        assertEquals("甲 乙 丙（剪辑）", row.getCell(4).getStringCellValue());
        assertEquals("被官方媒体转载", row.getCell(5).getStringCellValue());
        assertEquals(350d, row.getCell(8).getNumericCellValue());
        assertEquals(2d, row.getCell(9).getNumericCellValue());
        assertEquals(300d, row.getCell(10).getNumericCellValue());
        assertEquals(1d, row.getCell(11).getNumericCellValue());
        workbook.close();
    }

    @Test
    void writesPhotoCountsAndUnequalAmountsWithTemplateSeparators() throws Exception {
        FeeListVO item = item("PHOTO", "院网", "院网摄影稿");
        FeeDetailVO detail = detail(new LinkedHashMap<String, Object>() {{
            put("platform", "院网");
            put("publishDate", LocalDate.of(2026, 6, 25));
            put("articleLink", "https://example.com/photo");
        }}, contributor("甲", "SHOOTER", "30.0", 2), contributor("乙", "SHOOTER", "60.0", 5));

        Workbook workbook = writeAndRead(FeeTemplateExportWriter.PHOTO, item, detail);
        Row row = workbook.getSheetAt(0).getRow(3);
        assertEquals("校园网", row.getCell(1).getStringCellValue());
        assertEquals("甲 乙", row.getCell(4).getStringCellValue());
        assertEquals("2/5", row.getCell(6).getStringCellValue());
        assertEquals(90d, row.getCell(7).getNumericCellValue());
        assertEquals("30/60", row.getCell(8).getStringCellValue());
        workbook.close();
    }

    @Test
    void mapsWebsiteAndVisualCategoriesToExistingWorksheetWording() throws Exception {
        FeeListVO websiteItem = item("EDITORIAL", "院网", "院网稿");
        FeeDetailVO websiteDetail = detail(new LinkedHashMap<String, Object>() {{
            put("category", "学校新闻（含学校要闻、校园时讯）");
            put("publishDate", LocalDate.of(2026, 6, 26));
        }}, contributor("甲", "AUTHOR", "50.0", null));
        Workbook websiteWorkbook = writeAndRead(FeeTemplateExportWriter.EDITORIAL_WEBSITE, websiteItem, websiteDetail);
        assertEquals("学校新闻", websiteWorkbook.getSheetAt(0).getRow(3).getCell(1).getStringCellValue());
        websiteWorkbook.close();

        FeeListVO visualItem = item("VISUAL", null, "视觉稿");
        FeeDetailVO visualDetail = detail(new LinkedHashMap<String, Object>() {{
            put("category", "互动、长图、SVG动图、手绘");
            put("publishDate", LocalDate.of(2026, 6, 27));
        }}, contributor("乙", "DESIGNER", "50.0", null));
        Workbook visualWorkbook = writeAndRead(FeeTemplateExportWriter.VISUAL, visualItem, visualDetail);
        assertEquals("互动、长图、SVG、手绘", visualWorkbook.getSheetAt(0).getRow(3).getCell(1).getStringCellValue());
        visualWorkbook.close();
    }

    @Test
    void writesNewspaperWorkbookUsingReferenceColumnOrder() throws Exception {
        // 202609081435 - 验证采编校报固定模板的文章字数和备注列顺序与参考工作簿一致。
        FeeListVO newspaperItem = item("EDITORIAL", "校报", "校报稿");
        FeeDetailVO newspaperDetail = detail(new LinkedHashMap<String, Object>() {{
            put("category", "原创一般稿件");
            put("publishDate", LocalDate.of(2026, 6, 28));
            put("wordCount", 2800);
            put("articleLink", "https://example.com/newspaper");
            put("isSpecialNode", 1);
            put("specialMultiplier", 3);
        }}, contributor("甲", "AUTHOR", "80.0", null));
        newspaperDetail.setRemark("需人工复核");

        Workbook workbook = writeAndRead(FeeTemplateExportWriter.EDITORIAL_NEWSPAPER, newspaperItem, newspaperDetail);
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.getRow(3);
        assertEquals("校学生记者团稿费统计（校报）", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("文章字数", sheet.getRow(2).getCell(5).getStringCellValue());
        assertEquals(2800d, row.getCell(5).getNumericCellValue());
        assertEquals("是", row.getCell(10).getStringCellValue());
        assertEquals("3倍", row.getCell(11).getStringCellValue());
        assertEquals("需人工复核", row.getCell(12).getStringCellValue());
        assertEquals(new CellRangeAddress(0, 0, 0, 11).formatAsString(), sheet.getMergedRegion(0).formatAsString());
        workbook.close();
    }

    @Test
    void templateMatchingLimitsEachWorkbookToItsOwnDepartment() {
        assertTrue(FeeTemplateExportWriter.matches(FeeTemplateExportWriter.EDITORIAL_WECHAT,
                item("EDITORIAL", "微信", "微信稿")));
        assertFalse(FeeTemplateExportWriter.matches(FeeTemplateExportWriter.EDITORIAL_WECHAT,
                item("EDITORIAL", "院网", "院网稿")));
        assertTrue(FeeTemplateExportWriter.matches(FeeTemplateExportWriter.EDITORIAL_NEWSPAPER,
                item("EDITORIAL", "校报", "校报稿")));
        assertFalse(FeeTemplateExportWriter.matches(FeeTemplateExportWriter.PHOTO,
                item("VIDEO", null, "视频")));
    }

    private Workbook writeAndRead(String template, FeeListVO item, FeeDetailVO detail) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FeeTemplateExportWriter.write(output, template,
                Collections.singletonList(new FeeTemplateExportWriter.ExportItem(item, detail)), "2026-06");
        return new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()));
    }

    private FeeListVO item(String type, String platform, String title) {
        FeeListVO item = new FeeListVO();
        item.setId(1L);
        item.setWorkType(type);
        item.setPlatform(platform);
        item.setTitle(title);
        item.setPublishDate(LocalDate.of(2026, 6, 18));
        return item;
    }

    private FeeDetailVO detail(Map<String, Object> workInfo, FeeDetailVO.ContributorVO... contributors) {
        FeeDetailVO detail = new FeeDetailVO();
        detail.setWorkInfo(workInfo);
        detail.setContributors(Arrays.asList(contributors));
        detail.setTotalAmount(Arrays.stream(contributors).map(FeeDetailVO.ContributorVO::getFeeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return detail;
    }

    private FeeDetailVO.ContributorVO contributor(String name, String role, String amount, Integer workCount) {
        FeeDetailVO.ContributorVO contributor = new FeeDetailVO.ContributorVO();
        contributor.setRealName(name);
        contributor.setRoleInWork(role);
        contributor.setFeeAmount(new BigDecimal(amount));
        contributor.setWorkCount(workCount);
        return contributor;
    }
}
