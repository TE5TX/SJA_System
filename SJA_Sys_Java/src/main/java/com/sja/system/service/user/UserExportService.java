package com.sja.system.service.user;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.sja.system.application.model.UserExportFile;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.user.UserExportDTO;
import com.sja.system.application.vo.user.UserVO;
import com.sja.system.service.system.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coordinates user-information spreadsheet generation and its audit record.
 */
@Service
@RequiredArgsConstructor
public class UserExportService {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Map<String, ExportField> EXPORT_FIELDS = createExportFields();

    private final UserService userService;
    private final OperationLogService operationLogService;

    /**
     * Generates one user spreadsheet from the requested supported fields.
     *
     * @throws ApiException when an unsupported field is requested
     */
    // 202609181625 - 将人员导出字段校验、Excel 构建和审计下沉至应用服务。
    public Optional<UserExportFile> export(UserExportDTO dto, Long operatorId, String operatorName,
                                           Integer operatorRoleCode, String clientIp) {
        List<ExportField> selectedFields = resolveExportFields(dto.getFields());
        List<UserVO> users = userService.queryUsersForExport(dto);
        if (users.isEmpty()) {
            return Optional.empty();
        }

        byte[] content = writeWorkbook(selectedFields, users);
        String fileName = "人员信息_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        String role = operatorRoleCode == null ? "" : String.valueOf(operatorRoleCode);
        operationLogService.log(operatorId, operatorName, role, "EXPORT", "USER", null,
                "导出人员信息，共 " + users.size() + " 条，字段：" + dto.getFields(), clientIp);
        return Optional.of(new UserExportFile(fileName, XLSX_CONTENT_TYPE, content));
    }

    private List<ExportField> resolveExportFields(List<String> requestedFields) {
        List<ExportField> selectedFields = requestedFields.stream()
                .map(EXPORT_FIELDS::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (selectedFields.size() != requestedFields.stream().distinct().count()) {
            throw ApiException.badRequest("包含不支持的导出字段");
        }
        return selectedFields;
    }

    private byte[] writeWorkbook(List<ExportField> selectedFields, List<UserVO> users) {
        List<List<String>> head = selectedFields.stream()
                .map(field -> Collections.singletonList(field.label()))
                .toList();
        List<List<String>> data = users.stream()
                .map(user -> selectedFields.stream()
                        .map(field -> field.valueExtractor().apply(user))
                        .map(value -> value == null ? "" : value)
                        .collect(Collectors.toList()))
                .toList();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output)
                .head(head)
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                .sheet("人员信息")
                .doWrite(data);
        return output.toByteArray();
    }

    private static Map<String, ExportField> createExportFields() {
        Map<String, ExportField> fields = new LinkedHashMap<>();
        fields.put("realName", new ExportField("姓名", UserVO::getRealName));
        fields.put("gender", new ExportField("性别", user -> formatGender(user.getGender())));
        fields.put("studentNo", new ExportField("学号", UserVO::getStudentNo));
        fields.put("phone", new ExportField("手机号", UserVO::getPhone));
        fields.put("idCard", new ExportField("身份证号", UserVO::getIdCard));
        fields.put("bankCard", new ExportField("银行卡号", UserVO::getBankCard));
        fields.put("college", new ExportField("学院", UserVO::getCollege));
        fields.put("className", new ExportField("班级", UserVO::getClassName));
        fields.put("dormAddress", new ExportField("宿舍地址", UserVO::getDormAddress));
        fields.put("deptNames", new ExportField("部门", user -> join(user.getDeptNames())));
        fields.put("roleNames", new ExportField("角色", user -> join(user.getRoleNames())));
        fields.put("isValid", new ExportField("状态", user -> user.getIsValid() != null && user.getIsValid() == 1
                ? "启用" : "禁用"));
        fields.put("photo", new ExportField("照片地址", UserVO::getPhoto));
        fields.put("remark", new ExportField("备注", UserVO::getRemark));
        return Collections.unmodifiableMap(fields);
    }

    private static String formatGender(Integer gender) {
        if (gender == null || gender == 0) return "未知";
        return gender == 1 ? "男" : "女";
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join("、", values);
    }

    private record ExportField(String label, Function<UserVO, String> valueExtractor) { }
}
