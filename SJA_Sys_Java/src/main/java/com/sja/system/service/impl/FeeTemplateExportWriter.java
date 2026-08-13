package com.sja.system.service.impl;

import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Writes the fixed departmental workbooks used by the student press corps. */
final class FeeTemplateExportWriter {

    static final String EDITORIAL_WECHAT = "EDITORIAL_WECHAT";
    static final String EDITORIAL_WEBSITE = "EDITORIAL_WEBSITE";
    static final String PHOTO = "PHOTO";
    static final String VISUAL = "VISUAL";
    static final String VIDEO = "VIDEO";

    private static final Set<String> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(EDITORIAL_WECHAT, EDITORIAL_WEBSITE, PHOTO, VISUAL, VIDEO)));

    private static final Map<String, TemplateDefinition> DEFINITIONS = createDefinitions();

    private FeeTemplateExportWriter() {
    }

    static boolean supports(String templateType) {
        return SUPPORTED_TYPES.contains(templateType);
    }

    static boolean matches(String templateType, FeeListVO item) {
        if (item == null) return false;
        switch (templateType) {
            case EDITORIAL_WECHAT:
                return "EDITORIAL".equals(item.getWorkType()) && "微信".equals(item.getPlatform());
            case EDITORIAL_WEBSITE:
                return "EDITORIAL".equals(item.getWorkType()) && "院网".equals(item.getPlatform());
            case PHOTO:
                return "PHOTO".equals(item.getWorkType());
            case VISUAL:
                return "VISUAL".equals(item.getWorkType());
            case VIDEO:
                return "VIDEO".equals(item.getWorkType());
            default:
                return false;
        }
    }

    static void write(OutputStream outputStream, String templateType, List<ExportItem> items,
                      String selectedMonth) throws IOException {
        TemplateDefinition definition = DEFINITIONS.get(templateType);
        if (definition == null) throw new IllegalArgumentException("不支持的稿费导出模板: " + templateType);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Styles styles = new Styles(workbook);
            int lastColumn = definition.headers.size() - 1;

            createMergedRow(sheet, 0, lastColumn, definition.title, styles.title);
            createMergedRow(sheet, 1, lastColumn, definition.instructions, styles.instructions);
            createHeaderRow(sheet, definition.headers, styles.header);

            int rowIndex = 3;
            for (ExportItem item : items) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(36);
                List<Object> values = definition.rowMapper.apply(new RowContext(item, selectedMonth));
                for (int column = 0; column < definition.headers.size(); column++) {
                    Cell cell = row.createCell(column);
                    Object value = column < values.size() ? values.get(column) : null;
                    writeCell(workbook, cell, value, styles);
                }
            }

            for (int column = 0; column < definition.columnWidths.length; column++) {
                sheet.setColumnWidth(column, Math.min(255, definition.columnWidths[column]) * 256);
            }
            sheet.setColumnHidden(lastColumn, false);
            sheet.createFreezePane(0, 3);
            sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, lastColumn));
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            workbook.write(outputStream);
        }
    }

    private static void createMergedRow(Sheet sheet, int rowIndex, int lastColumn,
                                        String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(rowIndex == 0 ? 28 : 58);
        for (int column = 0; column <= lastColumn; column++) {
            Cell cell = row.createCell(column);
            cell.setCellStyle(style);
        }
        row.getCell(0).setCellValue(value);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
    }

    private static void createHeaderRow(Sheet sheet, List<String> headers, CellStyle style) {
        Row row = sheet.createRow(2);
        row.setHeightInPoints(28);
        for (int column = 0; column < headers.size(); column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(style);
        }
    }

    private static void writeCell(Workbook workbook, Cell cell, Object value, Styles styles) {
        if (value instanceof LocalDate) {
            cell.setCellValue((LocalDate) value);
            cell.setCellStyle(styles.date);
            return;
        }
        if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
            cell.setCellStyle(styles.money);
            return;
        }
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
            cell.setCellStyle(styles.data);
            return;
        }
        String text = value != null ? String.valueOf(value) : "";
        cell.setCellValue(text);
        if (text.startsWith("http://") || text.startsWith("https://")) {
            Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
            hyperlink.setAddress(text);
            cell.setHyperlink(hyperlink);
            cell.setCellStyle(styles.hyperlink);
        } else {
            cell.setCellStyle(styles.data);
        }
    }

    private static Map<String, TemplateDefinition> createDefinitions() {
        Map<String, TemplateDefinition> definitions = new LinkedHashMap<>();
        definitions.put(EDITORIAL_WECHAT, new TemplateDefinition(
                "校学生记者团稿费统计（微信）",
                "填表须知：1、板块分为原创及半原创类，常见四件套为半原创类；2、微信推文的计算仅计算【由微信端口】安排的任务，专访、部分院网负责的新闻稿等均不纳入该表的稿酬计算；3、请部长务必认真核对数据，若有漏算少算，概不重新处理。",
                Arrays.asList("月份", "板块", "文章标题", "发布日期", "作者姓名", "文章链接", "阅读量", "文章总额", "作者稿费均额", "人数"),
                new int[]{10, 20, 42, 14, 30, 48, 12, 13, 16, 8},
                FeeTemplateExportWriter::wechatRow));
        definitions.put(EDITORIAL_WEBSITE, new TemplateDefinition(
                "校学生记者团稿费统计（院网）",
                "填表须知：1、所有院网安排的专访均属“专题报道（含人物、事件类）”板块；2、无论首发在哪个平台，由院网安排的新闻稿均属“学校新闻”，不与微信平台共同计算稿费；3、请部长务必认真核对数据，若有漏算少算，概不重新处理。",
                Arrays.asList("月份", "板块", "文章标题", "发布日期", "作者姓名", "文章链接", "文章总额", "作者稿费均额", "人数"),
                new int[]{10, 25, 42, 14, 30, 48, 13, 16, 8},
                FeeTemplateExportWriter::websiteRow));
        definitions.put(PHOTO, new TemplateDefinition(
                "校学生记者团稿费统计（摄影）",
                "填表须知：1、发布平台不同稿费算法不同，微信平台填写阅读量、校报院网平台填写选用张数，填表前请核实图片首发平台；2、采用图片的数量按采编最终返回的插图图包数量为准，如老师端有变化会及时同步；3、请部长务必认真核对数据，若有漏算少算，概不重新处理。",
                Arrays.asList("月份", "发布平台", "文章标题", "发布日期", "作者姓名", "文章链接", "阅读量/图片分别选用张数", "文章总额", "作者稿费均额", "人数"),
                new int[]{10, 16, 42, 14, 34, 48, 28, 13, 26, 8},
                FeeTemplateExportWriter::photoRow));
        definitions.put(VISUAL, new TemplateDefinition(
                "校学生记者团稿费统计（视觉）",
                "填表须知：1、微信推文排版、大图等设计均单独统计，不与微信共同计算；2、板块中不含制图原创美文，可参考“燃炸！好样的”，部分制图原创美文可参考“军训微笑图集”；3、请部长务必认真核对数据，若有漏算少算，概不重新处理。",
                Arrays.asList("月份", "板块", "文章标题", "发布日期", "作者姓名", "文章链接", "文章总额", "作者稿费均额", "人数"),
                new int[]{10, 28, 42, 14, 30, 48, 13, 16, 8},
                FeeTemplateExportWriter::visualRow));
        definitions.put(VIDEO, new TemplateDefinition(
                "校学生记者团稿费统计（视频）",
                "填表须知：1、板块按视频部稿费标准填写；2、拍摄与剪辑人员分别统计，剪辑人员姓名后标注“（剪辑）”；3、请部长务必认真核对数据，若有漏算少算，概不重新处理。",
                Arrays.asList("月份", "板块", "文章标题", "发布日期", "作者姓名", "转载情况", "视频链接", "视频总额", "拍摄稿费均额", "拍摄人数", "剪辑稿费均额", "剪辑人数"),
                new int[]{10, 28, 42, 14, 38, 18, 48, 13, 16, 12, 16, 12},
                FeeTemplateExportWriter::videoRow));
        return Collections.unmodifiableMap(definitions);
    }

    private static List<Object> wechatRow(RowContext context) {
        return Arrays.asList(context.month(), editorialCategory(context.info().get("category")), context.item.item.getTitle(),
                context.publishDate(), context.names(context.contributors()), context.link(), context.info().get("readCount"),
                context.item.detail.getTotalAmount(), context.amountSummary(context.contributors()), context.contributors().size());
    }

    private static List<Object> websiteRow(RowContext context) {
        return Arrays.asList(context.month(), websiteCategory(context.info().get("category")), context.item.item.getTitle(),
                context.publishDate(), context.names(context.contributors()), context.link(), context.item.detail.getTotalAmount(),
                context.amountSummary(context.contributors()), context.contributors().size());
    }

    private static List<Object> photoRow(RowContext context) {
        String platform = value(context.info().get("platform"));
        Object quantity = "微信".equals(platform)
                ? context.info().get("readCount")
                : context.join(context.contributors(), contributor -> contributor.getWorkCount() != null
                ? String.valueOf(contributor.getWorkCount()) : "");
        return Arrays.asList(context.month(), photoPlatform(platform), context.item.item.getTitle(), context.publishDate(),
                context.names(context.contributors()), context.link(), quantity, context.item.detail.getTotalAmount(),
                context.amountSummary(context.contributors()), context.contributors().size());
    }

    private static List<Object> visualRow(RowContext context) {
        return Arrays.asList(context.month(), visualCategory(context.info().get("category")), context.item.item.getTitle(),
                context.publishDate(), context.names(context.contributors()), context.link(), context.item.detail.getTotalAmount(),
                context.amountSummary(context.contributors()), context.contributors().size());
    }

    private static List<Object> videoRow(RowContext context) {
        List<FeeDetailVO.ContributorVO> shooters = context.role("SHOOTER");
        List<FeeDetailVO.ContributorVO> editors = context.role("EDITOR");
        List<String> names = new ArrayList<>();
        shooters.stream().map(FeeDetailVO.ContributorVO::getRealName).filter(StringUtils::hasText).forEach(names::add);
        editors.stream().map(FeeDetailVO.ContributorVO::getRealName).filter(StringUtils::hasText)
                .map(name -> name + "（剪辑）").forEach(names::add);
        String category = value(context.info().get("category"));
        if (isTrue(context.info().get("isSpecialNode"))) category += "+特殊节点视频";
        return Arrays.asList(context.month(), category, context.item.item.getTitle(), context.publishDate(),
                String.join(" ", names), isTrue(context.info().get("isReposted")) ? "被官方媒体转载" : "无",
                context.link(), context.item.detail.getTotalAmount(), context.amountSummary(shooters), shooters.size(),
                context.amountSummary(editors), editors.size());
    }

    private static String editorialCategory(Object value) {
        String category = value(value);
        if ("原创类".equals(category)) return "原创类稿件";
        if ("半原创类".equals(category)) return "半原创类稿件";
        return category;
    }

    private static String websiteCategory(Object value) {
        String category = value(value);
        if ("学校新闻（含学校要闻、校园时讯）".equals(category)) return "学校新闻";
        return category;
    }

    private static String visualCategory(Object value) {
        String category = value(value);
        if ("原创美文（不含制图）".equals(category)) return "不含制图原创美文";
        if ("原创美文（含部分制图）".equals(category)) return "含部分制图原创美文";
        if ("互动、长图、SVG动图、手绘".equals(category)) return "互动、长图、SVG、手绘";
        if ("校园网首页大图".equals(category)) return "校园网大图";
        return category;
    }

    private static String photoPlatform(String platform) {
        if ("微信".equals(platform)) return "微信公众平台";
        if ("院网".equals(platform)) return "校园网";
        return platform;
    }

    private static boolean isTrue(Object value) {
        return Objects.equals(1, value) || Objects.equals("1", String.valueOf(value))
                || Objects.equals(Boolean.TRUE, value);
    }

    private static String value(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    static final class ExportItem {
        private final FeeListVO item;
        private final FeeDetailVO detail;

        ExportItem(FeeListVO item, FeeDetailVO detail) {
            this.item = item;
            this.detail = detail;
        }
    }

    private static final class RowContext {
        private final ExportItem item;
        private final String selectedMonth;

        private RowContext(ExportItem item, String selectedMonth) {
            this.item = item;
            this.selectedMonth = selectedMonth;
        }

        private Map<String, Object> info() {
            return item.detail.getWorkInfo() != null ? item.detail.getWorkInfo() : Collections.emptyMap();
        }

        private List<FeeDetailVO.ContributorVO> contributors() {
            return item.detail.getContributors() != null ? item.detail.getContributors() : Collections.emptyList();
        }

        private List<FeeDetailVO.ContributorVO> role(String role) {
            return contributors().stream().filter(contributor -> role.equals(contributor.getRoleInWork()))
                    .collect(Collectors.toList());
        }

        private String month() {
            LocalDate publishDate = publishDate();
            if (publishDate != null) return publishDate.getMonthValue() + "月";
            if (StringUtils.hasText(selectedMonth)) {
                try {
                    return YearMonth.parse(selectedMonth).getMonthValue() + "月";
                } catch (RuntimeException ignored) {
                    return selectedMonth;
                }
            }
            return "";
        }

        private LocalDate publishDate() {
            Object date = info().get("publishDate");
            if (date instanceof LocalDate) return (LocalDate) date;
            if (date != null && StringUtils.hasText(String.valueOf(date))) {
                try {
                    return LocalDate.parse(String.valueOf(date));
                } catch (RuntimeException ignored) {
                    return item.item.getPublishDate();
                }
            }
            return item.item.getPublishDate();
        }

        private String link() {
            return value(info().containsKey("videoLink") ? info().get("videoLink") : info().get("articleLink"));
        }

        private String names(List<FeeDetailVO.ContributorVO> contributors) {
            return contributors.stream().map(FeeDetailVO.ContributorVO::getRealName)
                    .filter(StringUtils::hasText).collect(Collectors.joining(" "));
        }

        private Object amountSummary(List<FeeDetailVO.ContributorVO> contributors) {
            List<BigDecimal> amounts = contributors.stream().map(FeeDetailVO.ContributorVO::getFeeAmount)
                    .filter(Objects::nonNull).map(amount -> amount.setScale(1, java.math.RoundingMode.HALF_UP))
                    .collect(Collectors.toList());
            if (amounts.isEmpty()) return "";
            if (amounts.stream().distinct().count() == 1) return amounts.get(0);
            return amounts.stream().map(FeeTemplateExportWriter::compactMoney).collect(Collectors.joining("/"));
        }

        private String join(List<FeeDetailVO.ContributorVO> contributors,
                            Function<FeeDetailVO.ContributorVO, String> extractor) {
            return contributors.stream().map(extractor).filter(StringUtils::hasText).collect(Collectors.joining("/"));
        }
    }

    private static String compactMoney(BigDecimal amount) {
        return amount.setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static final class TemplateDefinition {
        private final String title;
        private final String instructions;
        private final List<String> headers;
        private final int[] columnWidths;
        private final Function<RowContext, List<Object>> rowMapper;

        private TemplateDefinition(String title, String instructions, List<String> headers, int[] columnWidths,
                                   Function<RowContext, List<Object>> rowMapper) {
            this.title = title;
            this.instructions = instructions;
            this.headers = headers;
            this.columnWidths = columnWidths;
            this.rowMapper = rowMapper;
        }
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle instructions;
        private final CellStyle header;
        private final CellStyle data;
        private final CellStyle date;
        private final CellStyle money;
        private final CellStyle hyperlink;

        private Styles(Workbook workbook) {
            title = bordered(workbook);
            Font titleFont = workbook.createFont();
            titleFont.setFontName("方正小标宋_GBK");
            titleFont.setFontHeightInPoints((short) 14);
            title.setFont(titleFont);
            title.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            instructions = bordered(workbook);
            Font instructionFont = workbook.createFont();
            instructionFont.setFontName("等线");
            instructionFont.setFontHeightInPoints((short) 12);
            instructionFont.setColor(IndexedColors.RED.getIndex());
            instructions.setFont(instructionFont);
            instructions.setWrapText(true);

            header = bordered(workbook);
            Font headerFont = workbook.createFont();
            headerFont.setFontName("方正小标宋_GBK");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);
            header.setFont(headerFont);

            data = bordered(workbook);
            Font dataFont = workbook.createFont();
            dataFont.setFontName("等线");
            dataFont.setFontHeightInPoints((short) 11);
            data.setFont(dataFont);
            data.setWrapText(true);

            date = workbook.createCellStyle();
            date.cloneStyleFrom(data);
            date.setDataFormat(workbook.createDataFormat().getFormat("yyyy-m-d"));

            money = workbook.createCellStyle();
            money.cloneStyleFrom(data);
            money.setDataFormat(workbook.createDataFormat().getFormat("0.0"));

            hyperlink = workbook.createCellStyle();
            hyperlink.cloneStyleFrom(data);
            Font linkFont = workbook.createFont();
            linkFont.setFontName("等线");
            linkFont.setFontHeightInPoints((short) 11);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            linkFont.setUnderline(Font.U_SINGLE);
            hyperlink.setFont(linkFont);
        }

        private static CellStyle bordered(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }
}
