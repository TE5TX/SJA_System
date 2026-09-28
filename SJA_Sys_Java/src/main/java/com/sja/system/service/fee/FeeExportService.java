package com.sja.system.service.fee;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.sja.system.application.model.FeeExportFile;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.FeeExportDTO;
import com.sja.system.application.dto.fee.FeeTaskRefDTO;
import com.sja.system.dal.mysql.model.FeeListQuery;
import com.sja.system.service.system.OperationLogService;
import com.sja.system.application.vo.fee.FeeDetailVO;
import com.sja.system.application.vo.fee.FeeListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 稿费明细和固定模板文件生成服务。
 *
 * <p>本服务只生成与 HTTP 无关的文件模型，响应头仍由 Web 适配层统一处理。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
// 202609201644 - 将稿费模板选择、字段转换和Excel写入从综合服务中独立出来。
public class FeeExportService {

    private static final int MONEY_SCALE = 1;
    private static final int EXPORT_BATCH_SIZE = 200;
    private static final Set<String> TEMPLATE_TYPES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "EDITORIAL_WECHAT", "EDITORIAL_WEBSITE", "EDITORIAL_NEWSPAPER",
            "PHOTO", "VISUAL", "VIDEO", "SUBSIDY", "CUSTOM")));
    private static final Map<String, String> FIELD_LABELS = createFieldLabels();

    private final FeeQueryService queryService;
    private final FeeAccessService accessService;
    private final OperationLogService operationLogService;
    private final FeeFixedTemplatePort fixedTemplatePort;

    public FeeExportFile export(FeeExportDTO dto, Long operatorId, Integer roleCode) {
        accessService.ensureCanManageAmount(roleCode, "导出稿费表");
        List<String> fields = dto.getFields().stream().distinct().collect(Collectors.toList());
        if (fields.size() != dto.getFields().size() || !FIELD_LABELS.keySet().containsAll(fields)) {
            throw ApiException.badRequest("包含重复或不支持的导出字段");
        }
        String templateType = StringUtils.hasText(dto.getTemplateType()) ? dto.getTemplateType() : "CUSTOM";
        if (!TEMPLATE_TYPES.contains(templateType)) {
            throw ApiException.badRequest("不支持的稿费导出模板");
        }
        validateSelectedTasks(dto.getSelectedTasks());

        FeeListQuery query = queryService.createExportQuery(dto.getDeptCode(), dto.getMonth(), dto.getStatus(),
                dto.getContributorName(), dto.getCreateDate(), dto.getPublishStartDate(), dto.getPublishEndDate(),
                dto.getCreateStartDate(), dto.getCreateEndDate(), operatorId, roleCode, dto.getSelectedTasks());
        if (query == null) throw ApiException.forbidden("不能导出该部门的稿费任务");

        boolean fixedTemplate = fixedTemplatePort.supports(templateType);
        if (fixedTemplate && !queryService.applyTemplateConstraint(query, templateType)) {
            throw ApiException.badRequest("所选部门或平台与导出模板不匹配");
        }
        long count = queryService.count(query);
        if (dto.getSelectedTasks() != null && !dto.getSelectedTasks().isEmpty()
                && count != dto.getSelectedTasks().size()) {
            throw ApiException.badRequest(fixedTemplate
                    ? "部分选中任务与当前导出模板不匹配"
                    : "部分选中任务不存在、已失效或无权导出");
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (fixedTemplate) {
                fixedTemplatePort.write(output, templateType,
                        templateItems(query, operatorId, roleCode), dto.getMonth());
            } else {
                writeCustomExport(output, query, fields, operatorId, roleCode);
            }
            operationLogService.log(operatorId, "用户" + operatorId, String.valueOf(roleCode), "EXPORT",
                    "FEE", null, String.format("导出稿费明细 %d 条，模板：%s，字段：%s",
                            count, templateType, fields), "");
            String baseName = fixedTemplate ? templateFileName(templateType) : "校学生记者团稿费统计（自定义）";
            String fileName = baseName + "_" + (dto.getMonth() != null ? dto.getMonth() : "全部") + ".xlsx";
            return new FeeExportFile(fileName, FeeExportFile.XLSX_CONTENT_TYPE, output.toByteArray());
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("生成稿费导出文件失败", exception);
            throw new IllegalStateException("导出失败: " + exception.getMessage(), exception);
        }
    }

    private void validateSelectedTasks(List<FeeTaskRefDTO> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        Set<String> keys = tasks.stream().map(task -> task.getWorkType() + ":" + task.getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (keys.size() != tasks.size()) throw ApiException.badRequest("批量导出任务不能重复");
    }

    private Iterable<FeeFixedTemplatePort.ExportItem> templateItems(FeeListQuery query,
                                                                     Long operatorId,
                                                                     Integer roleCode) {
        return () -> new Iterator<>() {
            private long offset;
            private List<FeeListVO> batch = Collections.emptyList();
            private int index;
            private boolean finished;

            @Override
            public boolean hasNext() {
                while (index >= batch.size() && !finished) {
                    batch = queryService.loadBatch(query, offset, EXPORT_BATCH_SIZE);
                    offset += batch.size();
                    index = 0;
                    finished = batch.size() < EXPORT_BATCH_SIZE;
                }
                return index < batch.size();
            }

            @Override
            public FeeFixedTemplatePort.ExportItem next() {
                if (!hasNext()) throw new NoSuchElementException();
                FeeListVO item = batch.get(index++);
                return new FeeFixedTemplatePort.ExportItem(item,
                        queryService.detail(item.getId(), item.getWorkType(), operatorId, roleCode));
            }
        };
    }

    private void writeCustomExport(OutputStream output, FeeListQuery query, List<String> fields,
                                   Long operatorId, Integer roleCode) throws IOException {
        List<List<String>> head = fields.stream()
                .map(field -> Collections.singletonList(FIELD_LABELS.get(field)))
                .collect(Collectors.toList());
        try (ExcelWriter writer = EasyExcel.write(output)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).build()) {
            int sheetIndex = 0;
            for (FeeWorkType type : FeeWorkType.values()) {
                if (!query.getWorkTypes().contains(type.code())) continue;
                FeeListQuery typeQuery = queryService.copyForWorkType(query, type.code());
                long offset = 0;
                List<FeeListVO> batch = queryService.loadBatch(typeQuery, offset, EXPORT_BATCH_SIZE);
                if (batch.isEmpty()) continue;
                WriteSheet sheet = EasyExcel.writerSheet(sheetIndex++, sheetName(type)).head(head).build();
                while (!batch.isEmpty()) {
                    writer.write(buildRows(batch, fields, operatorId, roleCode), sheet);
                    offset += batch.size();
                    if (batch.size() < EXPORT_BATCH_SIZE) break;
                    batch = queryService.loadBatch(typeQuery, offset, EXPORT_BATCH_SIZE);
                }
            }
            if (sheetIndex == 0) {
                writer.write(Collections.emptyList(), EasyExcel.writerSheet(0, "稿费明细").head(head).build());
            }
        }
    }

    private List<List<String>> buildRows(List<FeeListVO> items, List<String> fields,
                                         Long operatorId, Integer roleCode) {
        List<List<String>> data = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (FeeListVO item : items) {
            FeeDetailVO detail = queryService.detail(item.getId(), item.getWorkType(), operatorId, roleCode);
            data.add(fields.stream().map(field -> fieldValue(field, item, detail)).collect(Collectors.toList()));
            if (detail.getTotalAmount() != null) total = total.add(detail.getTotalAmount());
        }
        List<String> totalRow = new ArrayList<>(Collections.nCopies(fields.size(), ""));
        int labelIndex = fields.indexOf("title");
        totalRow.set(labelIndex < 0 ? 0 : labelIndex, "合计");
        int amountIndex = fields.indexOf("totalAmount");
        if (amountIndex >= 0) totalRow.set(amountIndex, formatMoney(total));
        data.add(totalRow);
        return data;
    }

    private String fieldValue(String field, FeeListVO item, FeeDetailVO detail) {
        Map<String, Object> info = detail.getWorkInfo() != null ? detail.getWorkInfo() : Collections.emptyMap();
        List<FeeDetailVO.ContributorVO> contributors = detail.getContributors() != null
                ? detail.getContributors() : Collections.emptyList();
        return switch (field) {
            case "workType" -> categoryName(item.getWorkType());
            case "deptName" -> item.getDeptName();
            case "title" -> item.getTitle();
            case "platform" -> display(info.get("platform"));
            case "category" -> display(info.get("category"));
            // 202609281627 - 自定义导出不将未发布任务的内部归属日期误写为实际发布日期。
            case "publishDate" -> "0".equals(String.valueOf(info.get("isPublished")))
                    ? "-" : display(info.get("publishDate"));
            case "articleLink" -> display(info.containsKey("videoLink") ? info.get("videoLink") : info.get("articleLink"));
            case "isPublished" -> yesNo(info.get("isPublished"));
            case "readCount" -> display(info.get("readCount"));
            case "wordCount" -> display(info.get("wordCount"));
            case "videoDurationSec" -> display(info.get("videoDurationSec"));
            case "likeCount" -> display(info.get("likeCount"));
            case "isReposted" -> yesNo(info.get("isReposted"));
            case "isComplexEdit" -> yesNo(info.get("isComplexEdit"));
            case "subsidyMonth" -> display(info.get("subsidyMonth"));
            case "contributorNames" -> join(contributors, FeeDetailVO.ContributorVO::getRealName);
            case "contributorStudentNos" -> join(contributors, FeeDetailVO.ContributorVO::getStudentNo);
            case "contributorRoles" -> join(contributors, contributor -> roleName(contributor.getRoleInWork()));
            case "workCounts" -> join(contributors, contributor -> contributor.getWorkCount() != null
                    ? String.valueOf(contributor.getWorkCount()) : "");
            case "contributorAmounts" -> join(contributors, contributor -> formatMoney(contributor.getFeeAmount()));
            case "totalAmount" -> formatMoney(detail.getTotalAmount());
            case "auditStatus" -> statusName(detail.getStatus());
            case "isSpecialNode" -> yesNo(info.get("isSpecialNode"));
            case "specialMultiplier" -> display(info.get("specialMultiplier"));
            case "remark" -> display(detail.getRemark());
            case "createTime" -> item.getCreateTime() != null
                    ? item.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
            default -> throw ApiException.badRequest("不支持的导出字段: " + field);
        };
    }

    private String join(List<FeeDetailVO.ContributorVO> contributors,
                        Function<FeeDetailVO.ContributorVO, String> extractor) {
        return contributors.stream().map(extractor).map(this::display)
                .filter(StringUtils::hasText).collect(Collectors.joining("、"));
    }

    private String templateFileName(String type) {
        return switch (type) {
            case "EDITORIAL_WECHAT" -> "校学生记者团稿费统计（微信）";
            case "EDITORIAL_WEBSITE" -> "校学生记者团稿费统计（院网）";
            case "EDITORIAL_NEWSPAPER" -> "校学生记者团稿费统计（校报）";
            case "PHOTO" -> "校学生记者团稿费统计（摄影）";
            case "VISUAL" -> "校学生记者团稿费统计（视觉）";
            case "VIDEO" -> "校学生记者团稿费统计（视频）";
            default -> "稿费清单";
        };
    }

    private String sheetName(FeeWorkType type) {
        return type == FeeWorkType.SUBSIDY ? "主席团与小队" : type.departmentName();
    }

    private String categoryName(String type) {
        return switch (FeeWorkType.require(type)) {
            case EDITORIAL -> "采编稿件";
            case PHOTO -> "摄影作品";
            case VIDEO -> "视频作品";
            case VISUAL -> "视觉作品";
            case SUBSIDY -> "月度补贴";
        };
    }

    private String roleName(String role) {
        if ("AUTHOR".equals(role)) return "作者";
        if ("SHOOTER".equals(role)) return "拍摄";
        if ("EDITOR".equals(role)) return "编辑/剪辑";
        if ("DESIGNER".equals(role)) return "设计";
        if ("MEMBER".equals(role)) return "补贴人员";
        return display(role);
    }

    private String statusName(String status) {
        if ("APPROVED".equals(status)) return "已通过";
        if ("REJECTED".equals(status)) return "已驳回";
        return "待审核";
    }

    private String yesNo(Object value) {
        if (value == null) return "";
        int number;
        try {
            number = value instanceof Number ? ((Number) value).intValue()
                    : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            number = 0;
        }
        return number == 1 ? "是" : "否";
    }

    private String display(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "" : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private static Map<String, String> createFieldLabels() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("workType", "作品类型");
        fields.put("deptName", "部门");
        fields.put("title", "标题");
        fields.put("platform", "发布平台");
        fields.put("category", "类别");
        fields.put("publishDate", "发布时间");
        fields.put("articleLink", "作品链接");
        fields.put("isPublished", "是否已发布");
        fields.put("readCount", "阅读量");
        fields.put("wordCount", "字数");
        fields.put("videoDurationSec", "视频时长（秒）");
        fields.put("likeCount", "点赞数");
        fields.put("isReposted", "是否官方转载");
        fields.put("isComplexEdit", "是否复杂剪辑");
        fields.put("subsidyMonth", "补贴月份");
        fields.put("contributorNames", "参与人员姓名");
        fields.put("contributorStudentNos", "参与人员学号");
        fields.put("contributorRoles", "参与角色");
        fields.put("workCounts", "作品数量");
        fields.put("contributorAmounts", "个人金额");
        fields.put("totalAmount", "总金额");
        fields.put("auditStatus", "审核状态");
        fields.put("isSpecialNode", "特殊节点");
        fields.put("specialMultiplier", "特殊节点倍数");
        fields.put("remark", "备注");
        fields.put("createTime", "创建时间");
        return Collections.unmodifiableMap(fields);
    }
}
