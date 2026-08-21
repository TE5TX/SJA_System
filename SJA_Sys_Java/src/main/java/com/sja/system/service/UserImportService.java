package com.sja.system.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.dto.UserImportCandidateDTO;
import com.sja.system.dto.UserImportSnapshot;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.vo.UserImportPreviewVO;
import com.sja.system.vo.UserImportResultVO;
import com.sja.system.vo.UserImportRowVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private static final Set<Integer> IMPORT_OPERATOR_ROLES = Set.of(0, 1, 3);

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

    /** Parses the first worksheet and returns a non-mutating validation preview. */
    public UserImportPreviewVO preview(MultipartFile file, List<Long> deptIds, List<Long> roleIds,
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

    private ImportReadListener readWorkbook(MultipartFile file) {
        ImportReadListener listener = new ImportReadListener(maxRows);
        try {
            EasyExcel.read(file.getInputStream(), listener)
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

        if (operatorRoleCode != 0) {
            for (SysRole role : roles) {
                if (role.getRoleCode() == null || role.getRoleCode() < operatorRoleCode) {
                    throw ApiException.forbidden("不能批量注册权限高于当前用户的身份: " + role.getRoleName());
                }
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

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("请选择要导入的xlsx文件");
        if (file.getSize() > maxFileBytes) {
            throw ApiException.badRequest("xlsx文件不能超过 " + (maxFileBytes / 1024 / 1024) + "MB");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw ApiException.badRequest("只允许上传xlsx格式文件");
        }
    }

    private void requireAuthorizedOperator(Long operatorId, Integer operatorRoleCode) {
        if (operatorId == null) throw ApiException.unauthorized("未授权");
        if (operatorRoleCode == null || !IMPORT_OPERATOR_ROLES.contains(operatorRoleCode)) {
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
