package com.sja.system.service.user;

import com.alibaba.excel.EasyExcel;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.user.UserRosterSnapshot;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.application.vo.user.UserRosterImportResultVO;
import com.sja.system.service.system.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/** Coordinates a one-use roster confirmation without holding a batch transaction. */
// 202609181147 - 完成了人员名单确认导入、逐行跳过和基础设施故障中止统计。
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRosterImportService {

    private final UserImportService userImportService;
    private final UserRosterRowService rowService;
    private final UserService userService;
    private final OperationLogService operationLogService;

    public UserRosterImportResultVO confirm(String token, String currentPassword,
                                            Long operatorId, String operatorName,
                                            Integer operatorRoleCode, String operatorIp) {
        userImportService.requireAuthorizedOperator(operatorId, operatorRoleCode);
        userService.verifyCurrentPassword(operatorId, currentPassword);
        UserRosterSnapshot snapshot = userImportService.consumeRosterSnapshot(token, operatorId);
        SysRole selectedRole = userImportService.validateRosterRole(snapshot.roleId(), operatorRoleCode);
        List<UserRosterImportResultVO.Failure> failures = new ArrayList<>();
        for (UserRosterSnapshot.SkippedRow row : snapshot.skippedRows()) {
            failures.add(new UserRosterImportResultVO.Failure(
                    row.rowNumber(), row.realName(), row.studentNo(), row.reason()));
        }

        int imported = 0;
        int unprocessed = 0;
        String abortReason = null;
        List<UserRosterSnapshot.Candidate> candidates = snapshot.candidates();
        for (int index = 0; index < candidates.size(); index++) {
            UserRosterSnapshot.Candidate candidate = candidates.get(index);
            try {
                userImportService.validateRosterDepartments(candidate);
                // Separate proxied bean: REQUIRES_NEW is effective even when an earlier row failed.
                rowService.createCandidate(candidate, snapshot.roleId(),
                        operatorId, operatorName, operatorRoleCode);
                imported++;
            } catch (ApiException e) {
                if (e.getStatus() >= 500) {
                    unprocessed = candidates.size() - index;
                    abortReason = "服务暂不可用，请核查后重新预览剩余人员";
                    break;
                }
                failures.add(failure(candidate, businessReason(e)));
            } catch (DataIntegrityViolationException e) {
                failures.add(failure(candidate, "学号、手机号、身份证号码或关联记录发生冲突"));
            } catch (DataAccessException | TransactionException e) {
                unprocessed = candidates.size() - index;
                abortReason = "数据库暂不可用，请核查后重新预览剩余人员";
                log.error("Roster import aborted: operatorId={}, row={}, cause={}",
                        operatorId, candidate.rowNumber(), e.getClass().getSimpleName());
                break;
            } catch (RuntimeException e) {
                unprocessed = candidates.size() - index;
                abortReason = "处理异常，请核查后重新预览剩余人员";
                log.error("Roster import aborted: operatorId={}, row={}, cause={}",
                        operatorId, candidate.rowNumber(), e.getClass().getSimpleName());
                break;
            }
        }
        int skipped = failures.size();
        boolean aborted = unprocessed > 0;
        if (snapshot.totalCount() != imported + skipped + unprocessed) {
            throw new IllegalStateException("人员导入统计不一致");
        }
        recordSummary(operatorId, operatorName, operatorRoleCode, operatorIp, selectedRole.getRoleName(),
                imported, skipped, unprocessed, aborted);
        failures.sort(Comparator.comparingInt(UserRosterImportResultVO.Failure::rowNumber));
        String report = failures.isEmpty() ? null : createFailureReport(failures);
        return new UserRosterImportResultVO(snapshot.totalCount(), imported, skipped, unprocessed,
                aborted, abortReason, List.copyOf(failures), report);
    }

    private static UserRosterImportResultVO.Failure failure(UserRosterSnapshot.Candidate candidate, String reason) {
        return new UserRosterImportResultVO.Failure(
                candidate.rowNumber(), candidate.realName(), candidate.studentNo(), reason);
    }

    private static String businessReason(ApiException error) {
        return switch (error.getStatus()) {
            case 403 -> "无权分配该行身份";
            case 409 -> "学号、手机号或身份证号码已存在";
            default -> "字段、部门或身份状态已变化，请重新核对";
        };
    }

    private void recordSummary(Long operatorId, String operatorName, Integer roleCode, String ip,
                               String selectedRole, int imported, int skipped, int unprocessed,
                               boolean aborted) {
        try {
            operationLogService.log(operatorId, operatorName, String.valueOf(roleCode),
                    "IMPORT", "USER", null,
                    "人员名单批量导入：职务=" + selectedRole + "，成功=" + imported
                            + "，跳过=" + skipped + "，未处理=" + unprocessed
                            + "，中止=" + aborted, ip);
        } catch (RuntimeException e) {
            // Row transactions have already committed; an audit failure must not hide the actual result.
            log.error("Cannot persist roster import summary: operatorId={}, cause={}",
                    operatorId, e.getClass().getSimpleName());
        }
    }

    private String createFailureReport(List<UserRosterImportResultVO.Failure> failures) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            List<List<String>> rows = failures.stream().map(failure -> List.of(
                    String.valueOf(failure.rowNumber()), safeCell(failure.realName()),
                    safeCell(failure.studentNo()), safeCell(failure.reason()))).toList();
            EasyExcel.write(output)
                    .head(List.of(List.of("Excel行号"), List.of("姓名"), List.of("学号"), List.of("跳过原因")))
                    .sheet("导入失败明细").doWrite(rows);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (RuntimeException e) {
            log.error("Cannot create roster failure report: cause={}", e.getClass().getSimpleName());
            return null;
        }
    }

    /** Avoids spreadsheet formulas in exported user-controlled text. */
    private static String safeCell(String value) {
        if (value == null) return "";
        String normalized = value.length() > 500 ? value.substring(0, 500) : value;
        if (!normalized.isEmpty() && "=+-@\t\r\n".indexOf(normalized.charAt(0)) >= 0) {
            return "'" + normalized;
        }
        return normalized;
    }
}
