package com.sja.system.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.SheetWriteHandlerContext;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.common.ApiException;
import com.sja.system.authorization.RoleAuthorizationPolicy;
import com.sja.system.application.model.UploadedFile;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.dto.UserImportCandidateDTO;
import com.sja.system.dto.UserImportSnapshot;
import com.sja.system.dto.UserRosterSnapshot;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.vo.UserImportPreviewVO;
import com.sja.system.vo.UserImportResultVO;
import com.sja.system.vo.UserImportRowVO;
import com.sja.system.vo.UserRosterPreviewVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses and validates user import workbooks, stores short-lived previews in
 * Redis, and consumes each preview exactly once during registration.
 */
// 202608210907 - 实现了XLSX批量注册预览、Redis一次性令牌及事务确认导入。
@Slf4j
@Service
public class UserImportService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String EXPECTED_NAME_HEADER = "姓名";
    private static final String EXPECTED_STUDENT_HEADER = "学号";
    private static final List<String> BASIC_TEMPLATE_HEADERS = List.of("姓名", "学号");
    private static final List<String> ROSTER_TEMPLATE_HEADERS = List.of(
            "姓名", "学号", "二级学院", "年级专业班级", "手机号码", "身份证号码",
            "宿舍地址", "团内部门", "银行卡号");
    private static final List<Integer> ROSTER_TEXT_COLUMNS = List.of(1, 4, 5, 8);
    private static final Set<String> ROSTER_DEPARTMENT_NAMES = Set.of(
            "采编部", "摄影部", "视觉部", "视频部");
    private static final String ROSTER_KEY_SEGMENT = ":roster:v2:";

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final int tokenTtlSeconds;
    private final long maxFileBytes;
    private final int maxRows;

    public UserImportService(
            SysUserMapper sysUserMapper,
            SysDeptMapper sysDeptMapper,
            SysRoleMapper sysRoleMapper,
            UserService userService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${sja.user-import.key-prefix:sja:user-import:preview}") String keyPrefix,
            @Value("${sja.user-import.token-ttl-seconds:600}") int tokenTtlSeconds,
            @Value("${sja.user-import.max-file-bytes:2097152}") long maxFileBytes,
            @Value("${sja.user-import.max-rows:500}") int maxRows) {
        this.sysUserMapper = sysUserMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = requireConfiguredText(keyPrefix, "批量注册Redis前缀不能为空");
        this.tokenTtlSeconds = requirePositive(tokenTtlSeconds, "批量注册令牌有效期");
        this.maxFileBytes = requirePositive(maxFileBytes, "批量注册文件大小上限");
        this.maxRows = requirePositive(maxRows, "批量注册行数上限");
    }

    /** Keeps the existing two-column batch registration template available. */
    public byte[] createBasicTemplate() {
        return createTemplate(BASIC_TEMPLATE_HEADERS, "批量注册人员", false);
    }

    /** Creates the roster template; identifier columns use Excel text format. */
    public byte[] createRosterTemplate() {
        return createTemplate(ROSTER_TEMPLATE_HEADERS, "人员批量导入", true);
    }

    private byte[] createTemplate(List<String> headers, String sheetName, boolean formatIdentifiersAsText) {
        // 202609181445 - 统一生成两列与九列人员模板，避免新旧导入入口重复编写 Excel 逻辑。
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelWriterBuilder writer = EasyExcel.write(output)
                .head(headers.stream().map(Collections::singletonList).toList())
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(22));
        if (formatIdentifiersAsText) {
            writer.registerWriteHandler(new SheetWriteHandler() {
                @Override
                public void afterSheetCreate(SheetWriteHandlerContext context) {
                    var sheet = context.getWriteSheetHolder().getSheet();
                    var workbook = sheet.getWorkbook();
                    var textStyle = workbook.createCellStyle();
                    textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
                    ROSTER_TEXT_COLUMNS.forEach(index -> sheet.setDefaultColumnStyle(index, textStyle));
                }
            });
        }
        writer.sheet(sheetName).doWrite(Collections.emptyList());
        return output.toByteArray();
    }

    /** Parses the first worksheet and returns a non-mutating validation preview. */
    public UserImportPreviewVO preview(UploadedFile file, List<Long> deptIds, List<Long> roleIds,
                                       Long operatorId, Integer operatorRoleCode) {
        requireAuthorizedOperator(operatorId, operatorRoleCode);
        validateFile(file);

        Selection selection = validateSelection(deptIds, roleIds, operatorRoleCode);
        ImportReadListener listener = readWorkbook(file);
        List<UserImportCandidateDTO> candidates = listener.getCandidates();
        if (candidates.isEmpty()) {
            throw ApiException.badRequest("Excel中没有可导入的人员数据");
        }

        Map<String, Long> occurrences = candidates.stream()
                .filter(candidate -> StringUtils.hasText(candidate.getStudentNo()))
                .collect(Collectors.groupingBy(UserImportCandidateDTO::getStudentNo,
                        LinkedHashMap::new, Collectors.counting()));
        Set<String> existingStudentNos = findExistingStudentNos(occurrences.keySet());

        List<UserImportRowVO> rows = candidates.stream()
                .map(candidate -> validateCandidate(candidate, occurrences, existingStudentNos,
                        selection.deptNames(), selection.roleNames()))
                .toList();
        int validCount = (int) rows.stream().filter(row -> Boolean.TRUE.equals(row.getValid())).count();
        int invalidCount = rows.size() - validCount;

        String token = null;
        if (invalidCount == 0) {
            token = issueToken(new UserImportSnapshot(operatorId, operatorRoleCode,
                    selection.deptIds(), selection.roleIds(), candidates));
        }
        return new UserImportPreviewVO(token, invalidCount == 0 ? (long) tokenTtlSeconds : null,
                rows.size(), validCount, invalidCount, rows);
    }

    /** Validates a nine-column roster without writing any user or relationship records. */
    // 202609181104 - 完成了乱序表头、可选字段和逐行跳过的人员名单预览。
    public UserRosterPreviewVO previewRoster(UploadedFile file, Long roleId,
                                             Long operatorId, Integer operatorRoleCode) {
        requireAuthorizedOperator(operatorId, operatorRoleCode);
        validateFile(file);
        SysRole selectedRole = validateRosterRole(roleId, operatorRoleCode);
        RosterWorkbook roster = readRosterWorkbook(file);
        if (roster.rows().isEmpty()) throw ApiException.badRequest("Excel中没有可导入的人员数据");

        // 202609181445 - 将团内部门限定为四个业务部门，并移除名单中的成员身份列。
        Map<String, SysDept> departments = sysDeptMapper.selectList(new QueryWrapper<SysDept>()
                        .eq("is_valid", 1).in("dept_name", ROSTER_DEPARTMENT_NAMES)).stream()
                .collect(Collectors.toMap(SysDept::getDeptName, dept -> dept, (first, ignored) -> first));
        ExistingRosterValues existing = findExistingRosterValues(roster.rows());
        Set<String> seenStudents = new HashSet<>();
        Set<String> seenPhones = new HashSet<>();
        Set<String> seenIdCards = new HashSet<>();
        List<UserRosterPreviewVO.Row> previewRows = new ArrayList<>();
        List<UserRosterSnapshot.Candidate> accepted = new ArrayList<>();
        List<UserRosterSnapshot.SkippedRow> skippedRows = new ArrayList<>();
        for (RosterRawRow row : roster.rows()) {
            List<String> errors = new ArrayList<>(row.cellErrors());
            String name = row.value("姓名");
            String student = row.value("学号");
            String phone = row.value("手机号码");
            String idCard = row.value("身份证号码").toUpperCase(Locale.ROOT);
            String bankCard = row.value("银行卡号");
            checkRequired(name, 50, "姓名", errors);
            checkRequired(student, 20, "学号", errors);
            if (!student.isEmpty() && !student.matches("\\d{1,20}")) errors.add("学号必须为1到20位数字");
            if (idCard.isEmpty()) errors.add("身份证号码不能为空");
            else if (!idCard.matches("\\d{17}[\\dX]")) errors.add("身份证号码格式不正确");
            checkOptional(row.value("二级学院"), 100, "二级学院", errors);
            checkOptional(row.value("年级专业班级"), 50, "年级专业班级", errors);
            checkOptional(row.value("宿舍地址"), 100, "宿舍地址", errors);
            if (!phone.isEmpty() && !phone.matches("1[3-9]\\d{9}")) errors.add("手机号码格式不正确");
            if (!bankCard.isEmpty() && !bankCard.matches("\\d{10,30}")) errors.add("银行卡号格式不正确");

            List<Long> deptIds = new ArrayList<>();
            List<String> deptNames = new ArrayList<>();
            String deptCell = row.value("团内部门");
            if (!deptCell.isEmpty()) {
                for (String part : deptCell.split("、", -1)) {
                    String deptName = part.trim();
                    SysDept dept = departments.get(deptName);
                    if (!ROSTER_DEPARTMENT_NAMES.contains(deptName)) {
                        errors.add("团内部门只能填写采编部、摄影部、视觉部或视频部");
                    } else if (dept == null) {
                        errors.add("团内部门不存在或已禁用：" + deptName);
                    } else if (!deptIds.contains(dept.getId())) {
                        deptIds.add(dept.getId());
                        deptNames.add(deptName);
                    }
                }
            }
            if (!student.isEmpty()) {
                if (seenStudents.contains(student)) errors.add("学号在文件中重复");
                if (seenPhones.contains(student)) errors.add("学号与文件中手机号码冲突");
                if (existing.studentNos().contains(student)) errors.add("学号或登录账号已存在");
            }
            if (!phone.isEmpty()) {
                if (seenPhones.contains(phone)) errors.add("手机号码在文件中重复");
                if (seenStudents.contains(phone)) errors.add("手机号码与文件中学号冲突");
                if (existing.phones().contains(phone)) errors.add("手机号码已存在");
            }
            if (!idCard.isEmpty()) {
                if (seenIdCards.contains(idCard)) errors.add("身份证号码在文件中重复");
                if (existing.idCards().contains(idCard)) errors.add("身份证号码已存在");
            }
            boolean valid = errors.isEmpty();
            if (valid) {
                seenStudents.add(student);
                if (!phone.isEmpty()) seenPhones.add(phone);
                seenIdCards.add(idCard);
                accepted.add(new UserRosterSnapshot.Candidate(row.rowNumber(), name, student,
                        emptyToNull(row.value("二级学院")), emptyToNull(row.value("年级专业班级")),
                        emptyToNull(phone), idCard, emptyToNull(row.value("宿舍地址")),
                        emptyToNull(bankCard), deptIds));
            } else {
                skippedRows.add(new UserRosterSnapshot.SkippedRow(row.rowNumber(), name, student,
                        String.join("；", errors)));
            }
            previewRows.add(new UserRosterPreviewVO.Row(row.rowNumber(), name, student,
                    deptNames, valid, errors));
        }
        String token = accepted.isEmpty() ? null : issueRosterToken(
                new UserRosterSnapshot(operatorId, roleId, previewRows.size(), accepted, skippedRows));
        return new UserRosterPreviewVO(token, token == null ? null : (long) tokenTtlSeconds,
                previewRows.size(), accepted.size(), previewRows.size() - accepted.size(),
                selectedRole.getRoleName(), roster.ignoredHeaders(), previewRows);
    }

    /** Consumes a preview token and registers all rows in one database transaction. */
    @Transactional(rollbackFor = Exception.class)
    public UserImportResultVO confirm(String importToken, String currentPassword,
                                      Long operatorId, String operatorName,
                                      Integer operatorRoleCode) {
        requireAuthorizedOperator(operatorId, operatorRoleCode);
        userService.verifyCurrentPassword(operatorId, currentPassword);
        UserImportSnapshot snapshot = consumeSnapshot(importToken, operatorId);
        if (!operatorId.equals(snapshot.getOperatorId())) {
            throw ApiException.forbidden("该导入确认令牌不属于当前用户");
        }

        // Revalidate mutable database state and role hierarchy immediately before writing.
        validateSelection(snapshot.getDeptIds(), snapshot.getRoleIds(), operatorRoleCode);
        Set<String> studentNos = snapshot.getCandidates().stream()
                .map(UserImportCandidateDTO::getStudentNo)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> existingStudentNos = findExistingStudentNos(studentNos);
        if (!existingStudentNos.isEmpty()) {
            throw ApiException.conflict("预览后已有学号被注册，请重新上传并预览: "
                    + existingStudentNos.stream().limit(5).collect(Collectors.joining("、")));
        }

        List<Long> userIds = new ArrayList<>(snapshot.getCandidates().size());
        for (UserImportCandidateDTO candidate : snapshot.getCandidates()) {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setRealName(candidate.getRealName());
            dto.setStudentNo(candidate.getStudentNo());
            dto.setDeptIds(snapshot.getDeptIds());
            dto.setRoleIds(snapshot.getRoleIds());
            dto.setIsValid(1);
            userIds.add(userService.createUser(dto, operatorId, operatorName,
                    String.valueOf(operatorRoleCode)));
        }
        log.info("User import completed: operator={}, imported={}", operatorId, userIds.size());
        return new UserImportResultVO(userIds.size(), userIds);
    }

    private ImportReadListener readWorkbook(UploadedFile file) {
        ImportReadListener listener = new ImportReadListener(maxRows);
        try {
            EasyExcel.read(file.inputStream(), listener)
                    .headRowNumber(1)
                    .autoCloseStream(true)
                    .sheet(0)
                    .doRead();
            listener.assertHeaderRead();
            return listener;
        } catch (IOException e) {
            throw ApiException.badRequest("无法读取上传的Excel文件");
        } catch (RuntimeException e) {
            ApiException apiException = findApiException(e);
            if (apiException != null) throw apiException;
            log.warn("Cannot parse user import workbook: {}", e.getMessage());
            throw ApiException.badRequest("Excel文件格式不正确或内容无法解析");
        }
    }

    SysRole validateRosterRole(Long roleId, Integer operatorRoleCode) {
        if (roleId == null || roleId <= 0) throw ApiException.badRequest("请先选择本批人员的团内职务");
        SysRole role = loadActiveRoles(List.of(roleId)).get(0);
        if (!isAssignableRosterRole(role, operatorRoleCode)) {
            throw ApiException.forbidden("无权批量分配所选团内职务");
        }
        return role;
    }

    private boolean isAssignableRosterRole(SysRole role, Integer operatorRoleCode) {
        return RoleAuthorizationPolicy.canAssignRosterRole(operatorRoleCode, role.getRoleCode());
    }

    /** Rechecks department status and allowed names after the preview was created. */
    // 202609181445 - 确认导入时再次校验部门仍启用且名称属于四个允许选项。
    void validateRosterDepartments(UserRosterSnapshot.Candidate candidate) {
        if (candidate.deptIds() == null) throw ApiException.badRequest("导入快照的部门信息不完整");
        if (candidate.deptIds().size() > ROSTER_DEPARTMENT_NAMES.size()) {
            throw ApiException.badRequest("团内部门最多只能选择四个允许选项");
        }
        if (candidate.deptIds().isEmpty()) return;
        for (SysDept dept : loadActiveDepts(candidate.deptIds())) {
            if (!ROSTER_DEPARTMENT_NAMES.contains(dept.getDeptName())) {
                throw ApiException.badRequest("团内部门只能填写采编部、摄影部、视觉部或视频部");
            }
        }
    }

    /** Atomically consumes a roster-only preview token owned by the current operator. */
    UserRosterSnapshot consumeRosterSnapshot(String token, Long operatorId) {
        if (!StringUtils.hasText(token) || !token.matches("[A-Za-z0-9_-]{32,128}")) {
            throw ApiException.badRequest("导入确认令牌格式不正确");
        }
        try {
            String json = redisTemplate.opsForValue().getAndDelete(
                    rosterRedisKey(operatorId, token));
            if (!StringUtils.hasText(json)) {
                throw ApiException.badRequest("导入确认令牌已过期或已使用，请重新上传预览");
            }
            UserRosterSnapshot snapshot = objectMapper.readValue(json, UserRosterSnapshot.class);
            if (!operatorId.equals(snapshot.operatorId()) || snapshot.candidates() == null
                    || snapshot.candidates().isEmpty() || snapshot.skippedRows() == null
                    || snapshot.totalCount() != snapshot.candidates().size() + snapshot.skippedRows().size()) {
                throw ApiException.badRequest("导入快照已失效，请重新上传预览");
            }
            return snapshot;
        } catch (DataAccessException | JsonProcessingException e) {
            log.error("Cannot consume roster preview", e);
            throw ApiException.serviceUnavailable("人员名单确认服务暂不可用，请稍后重试");
        }
    }

    private RosterWorkbook readRosterWorkbook(UploadedFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.inputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) throw ApiException.badRequest("Excel第一行缺少表头");
            Map<String, Integer> columns = new LinkedHashMap<>();
            List<String> ignoredHeaders = new ArrayList<>();
            for (Cell cell : header) {
                String name = normalizeRosterHeader(new DataFormatter(Locale.CHINA).formatCellValue(cell));
                if (name.isEmpty()) continue;
                if ("正式部员/留任部员".equals(name)) {
                    throw ApiException.badRequest("已移除“正式部员/留任部员”列，请下载九列新模板并删除旧列");
                }
                if (ROSTER_TEMPLATE_HEADERS.contains(name)) {
                    if (columns.putIfAbsent(name, cell.getColumnIndex()) != null) {
                        throw ApiException.badRequest("Excel表头重复：" + name);
                    }
                } else ignoredHeaders.add(name);
            }
            List<String> missing = ROSTER_TEMPLATE_HEADERS.stream()
                    .filter(name -> !columns.containsKey(name)).toList();
            if (!missing.isEmpty()) throw ApiException.badRequest("Excel缺少表头：" + String.join("、", missing));
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            List<RosterRawRow> rows = new ArrayList<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row excelRow = sheet.getRow(index);
                if (excelRow == null) continue;
                Map<String, String> values = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
                boolean nonBlank = false;
                for (String headerName : ROSTER_TEMPLATE_HEADERS) {
                    Cell cell = excelRow.getCell(columns.get(headerName));
                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        values.put(headerName, "");
                        continue;
                    }
                    String value = formatter.formatCellValue(cell).trim();
                    values.put(headerName, value);
                    if (!value.isEmpty()) nonBlank = true;
                    if (cell.getCellType() == CellType.ERROR) {
                        errors.add(headerName + "包含错误单元格");
                    } else if (cell.getCellType() == CellType.FORMULA) {
                        errors.add(headerName + "不允许使用公式单元格");
                    } else if (cell.getCellType() == CellType.NUMERIC
                            && Set.of("学号", "手机号码", "身份证号码", "银行卡号").contains(headerName)) {
                        errors.add(headerName + "必须设置为文本格式后重新填写");
                    }
                }
                if (!nonBlank) continue;
                if (rows.size() >= maxRows) throw ApiException.badRequest("单次最多导入" + maxRows + "人");
                rows.add(new RosterRawRow(index + 1, values, errors));
            }
            return new RosterWorkbook(rows, ignoredHeaders);
        } catch (IOException | RuntimeException e) {
            ApiException apiException = findApiException(e);
            if (apiException != null) throw apiException;
            log.warn("Cannot parse roster workbook: {}", e.getClass().getSimpleName());
            throw ApiException.badRequest("Excel文件格式不正确或内容无法解析");
        }
    }

    private ExistingRosterValues findExistingRosterValues(List<RosterRawRow> rows) {
        Set<String> students = rows.stream().map(row -> row.value("学号"))
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        Set<String> phones = rows.stream().map(row -> row.value("手机号码"))
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        Set<String> cards = rows.stream().map(row -> row.value("身份证号码").toUpperCase(Locale.ROOT))
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        Set<String> foundStudents = findExistingStudentNos(students);
        Set<String> foundPhones = new HashSet<>();
        Set<String> foundCards = new HashSet<>();
        if (!phones.isEmpty()) {
            QueryWrapper<SysUser> phoneQuery = new QueryWrapper<SysUser>().select("phone", "username")
                    .and(group -> group.in("phone", phones).or().in("username", phones));
            for (SysUser user : sysUserMapper.selectList(phoneQuery)) {
                if (phones.contains(user.getPhone())) foundPhones.add(user.getPhone());
                if (phones.contains(user.getUsername())) foundPhones.add(user.getUsername());
            }
        }
        if (!cards.isEmpty()) {
            QueryWrapper<SysUser> cardQuery = new QueryWrapper<SysUser>().select("id_card").in("id_card", cards);
            for (SysUser user : sysUserMapper.selectList(cardQuery)) {
                if (user.getIdCard() != null) foundCards.add(user.getIdCard().toUpperCase(Locale.ROOT));
            }
        }
        return new ExistingRosterValues(foundStudents, foundPhones, foundCards);
    }

    private String issueRosterToken(UserRosterSnapshot snapshot) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        try {
            // 202609181445 - 为九列快照升级令牌命名空间，旧十列预览必须重新上传。
            redisTemplate.opsForValue().set(rosterRedisKey(snapshot.operatorId(), token),
                    objectMapper.writeValueAsString(snapshot), Duration.ofSeconds(tokenTtlSeconds));
            return token;
        } catch (DataAccessException | JsonProcessingException e) {
            log.error("Cannot persist roster preview", e);
            throw ApiException.serviceUnavailable("人员名单预览服务暂不可用，请稍后重试");
        }
    }

    private static String normalizeRosterHeader(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").replace("\u3000", " ").trim();
    }

    private static void checkRequired(String value, int maxLength, String label, List<String> errors) {
        if (value.isEmpty()) errors.add(label + "不能为空");
        else checkOptional(value, maxLength, label, errors);
    }

    private static void checkOptional(String value, int maxLength, String label, List<String> errors) {
        if (value.length() > maxLength) errors.add(label + "不能超过" + maxLength + "个字符");
        if (containsControlCharacter(value)) errors.add(label + "包含不允许的控制字符");
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private record RosterRawRow(int rowNumber, Map<String, String> values, List<String> cellErrors) {
        private String value(String name) { return values.getOrDefault(name, ""); }
    }

    private record RosterWorkbook(List<RosterRawRow> rows, List<String> ignoredHeaders) { }
    private record ExistingRosterValues(Set<String> studentNos, Set<String> phones, Set<String> idCards) { }

    private UserImportRowVO validateCandidate(UserImportCandidateDTO candidate,
                                              Map<String, Long> occurrences,
                                              Set<String> existingStudentNos,
                                              List<String> deptNames,
                                              List<String> roleNames) {
        List<String> errors = new ArrayList<>();
        String realName = candidate.getRealName();
        String studentNo = candidate.getStudentNo();
        if (!StringUtils.hasText(realName)) {
            errors.add("姓名不能为空");
        } else if (realName.length() > 50) {
            errors.add("姓名不能超过50个字符");
        } else if (containsControlCharacter(realName)) {
            errors.add("姓名包含不允许的控制字符");
        }
        if (!StringUtils.hasText(studentNo)) {
            errors.add("学号不能为空");
        } else if (!studentNo.matches("^\\d{1,20}$")) {
            errors.add("学号必须为1到20位数字");
        } else {
            if (occurrences.getOrDefault(studentNo, 0L) > 1) errors.add("学号在文件中重复");
            if (existingStudentNos.contains(studentNo)) errors.add("学号或登录账号已存在");
        }
        return new UserImportRowVO(candidate.getRowNumber(), realName, studentNo,
                deptNames, roleNames, errors.isEmpty(), errors);
    }

    private Selection validateSelection(List<Long> requestedDeptIds, List<Long> requestedRoleIds,
                                        Integer operatorRoleCode) {
        List<Long> deptIds = normalizeIds(requestedDeptIds, 10, "部门");
        List<Long> roleIds = normalizeIds(requestedRoleIds, 20, "身份");
        List<SysDept> depts = loadActiveDepts(deptIds);
        List<SysRole> roles = loadActiveRoles(roleIds);

        for (SysRole role : roles) {
            if (!RoleAuthorizationPolicy.canAssignRole(operatorRoleCode, role.getRoleCode())) {
                throw ApiException.forbidden("不能批量注册权限高于当前用户的身份: " + role.getRoleName());
            }
        }
        return new Selection(deptIds, roleIds,
                depts.stream().map(SysDept::getDeptName).toList(),
                roles.stream().map(SysRole::getRoleName).toList());
    }

    private List<SysDept> loadActiveDepts(List<Long> deptIds) {
        Map<Long, SysDept> byId = sysDeptMapper.selectBatchIds(deptIds).stream()
                .filter(dept -> Integer.valueOf(1).equals(dept.getIsValid()))
                .collect(Collectors.toMap(SysDept::getId, dept -> dept));
        if (byId.size() != deptIds.size()) throw ApiException.badRequest("包含不存在或已禁用的部门");
        return deptIds.stream().map(byId::get).toList();
    }

    private List<SysRole> loadActiveRoles(List<Long> roleIds) {
        Map<Long, SysRole> byId = sysRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> Integer.valueOf(1).equals(role.getIsValid()))
                .collect(Collectors.toMap(SysRole::getId, role -> role));
        if (byId.size() != roleIds.size()) throw ApiException.badRequest("包含不存在或已禁用的身份");
        return roleIds.stream().map(byId::get).toList();
    }

    private Set<String> findExistingStudentNos(Set<String> studentNos) {
        if (studentNos.isEmpty()) return Collections.emptySet();
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.select("student_no", "username")
                .and(wrapper -> wrapper.in("student_no", studentNos).or().in("username", studentNos));
        Set<String> existing = new HashSet<>();
        for (SysUser user : sysUserMapper.selectList(query)) {
            if (studentNos.contains(user.getStudentNo())) existing.add(user.getStudentNo());
            if (studentNos.contains(user.getUsername())) existing.add(user.getUsername());
        }
        return existing;
    }

    private String issueToken(UserImportSnapshot snapshot) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        try {
            redisTemplate.opsForValue().set(redisKey(snapshot.getOperatorId(), token),
                    objectMapper.writeValueAsString(snapshot),
                    Duration.ofSeconds(tokenTtlSeconds));
            return token;
        } catch (DataAccessException | JsonProcessingException e) {
            log.error("Cannot persist user import preview", e);
            throw ApiException.serviceUnavailable("批量注册预览服务暂不可用，请稍后重试");
        }
    }

    private UserImportSnapshot consumeSnapshot(String token, Long operatorId) {
        if (!StringUtils.hasText(token)) throw ApiException.badRequest("导入确认令牌不能为空");
        try {
            String json = redisTemplate.opsForValue().getAndDelete(redisKey(operatorId, token));
            if (!StringUtils.hasText(json)) {
                throw ApiException.badRequest("导入确认令牌已过期或已使用，请重新上传预览");
            }
            return objectMapper.readValue(json, UserImportSnapshot.class);
        } catch (DataAccessException | JsonProcessingException e) {
            log.error("Cannot consume user import preview", e);
            throw ApiException.serviceUnavailable("批量注册确认服务暂不可用，请稍后重试");
        }
    }

    private void validateFile(UploadedFile file) {
        if (file == null || file.empty()) throw ApiException.badRequest("请选择要导入的xlsx文件");
        if (file.size() > maxFileBytes) {
            throw ApiException.badRequest("xlsx文件不能超过 " + (maxFileBytes / 1024 / 1024) + "MB");
        }
        String filename = file.originalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw ApiException.badRequest("只允许上传xlsx格式文件");
        }
    }

    void requireAuthorizedOperator(Long operatorId, Integer operatorRoleCode) {
        if (operatorId == null) throw ApiException.unauthorized("未授权");
        if (!RoleAuthorizationPolicy.canImportUsers(operatorRoleCode)) {
            throw ApiException.forbidden("只有管理员、主席或秘书长可以批量注册人员");
        }
    }

    private List<Long> normalizeIds(List<Long> ids, int maxSize, String label) {
        if (ids == null || ids.isEmpty()) throw ApiException.badRequest("请至少选择一个" + label);
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw ApiException.badRequest(label + "ID不正确");
        }
        List<Long> normalized = new ArrayList<>(new LinkedHashSet<>(ids));
        if (normalized.size() > maxSize) throw ApiException.badRequest(label + "数量不能超过" + maxSize + "个");
        return normalized;
    }

    private String redisKey(Long operatorId, String token) {
        return keyPrefix + ":" + operatorId + ":" + token;
    }

    private String rosterRedisKey(Long operatorId, String token) {
        return keyPrefix + ROSTER_KEY_SEGMENT + operatorId + ":" + token;
    }

    private static boolean containsControlCharacter(String value) {
        return value.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint));
    }

    private static ApiException findApiException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ApiException apiException) return apiException;
            current = current.getCause();
        }
        return null;
    }

    private static int requirePositive(int value, String label) {
        if (value <= 0) throw new IllegalArgumentException(label + "必须大于0");
        return value;
    }

    private static long requirePositive(long value, String label) {
        if (value <= 0) throw new IllegalArgumentException(label + "必须大于0");
        return value;
    }

    private static String requireConfiguredText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private record Selection(List<Long> deptIds, List<Long> roleIds,
                             List<String> deptNames, List<String> roleNames) {
    }

    private static final class ImportReadListener extends AnalysisEventListener<Map<Integer, String>> {
        private final int maxRows;
        private final List<UserImportCandidateDTO> candidates = new ArrayList<>();
        private boolean headerRead;

        private ImportReadListener(int maxRows) {
            this.maxRows = maxRows;
        }

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            headerRead = true;
            String nameHeader = normalizeCell(headMap.get(0));
            String studentHeader = normalizeCell(headMap.get(1));
            if (!EXPECTED_NAME_HEADER.equals(nameHeader) || !EXPECTED_STUDENT_HEADER.equals(studentHeader)) {
                throw ApiException.badRequest("Excel第一行必须依次为：姓名、学号");
            }
            boolean hasUnexpectedHeader = headMap.entrySet().stream()
                    .anyMatch(entry -> entry.getKey() > 1 && StringUtils.hasText(normalizeCell(entry.getValue())));
            if (hasUnexpectedHeader) throw ApiException.badRequest("Excel只能包含姓名和学号两列");
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            String realName = normalizeCell(data.get(0));
            String studentNo = normalizeCell(data.get(1));
            if (!StringUtils.hasText(realName) && !StringUtils.hasText(studentNo)) return;
            if (candidates.size() >= maxRows) {
                throw ApiException.badRequest("单次最多导入" + maxRows + "人");
            }
            candidates.add(new UserImportCandidateDTO(
                    context.readRowHolder().getRowIndex() + 1, realName, studentNo));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // No-op: validation is performed after the complete first sheet is read.
        }

        private void assertHeaderRead() {
            if (!headerRead) throw ApiException.badRequest("Excel缺少表头，第一行必须为：姓名、学号");
        }

        private List<UserImportCandidateDTO> getCandidates() {
            return List.copyOf(candidates);
        }

        private static String normalizeCell(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
