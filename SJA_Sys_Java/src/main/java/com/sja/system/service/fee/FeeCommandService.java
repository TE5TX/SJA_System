package com.sja.system.service.fee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.service.FeeCalculationService;
import com.sja.system.service.OperationLogService;
import com.sja.system.vo.FeeDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 稿费提交、修改和试算用例服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
// 202609201644 - 将稿费写入编排从综合服务中独立出来，并通过作品适配器访问五类作品表。
public class FeeCommandService {

    private static final ObjectMapper AUDIT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final Map<String, String> AUDIT_FIELDS = createAuditFields();

    private final FeeAccessService accessService;
    private final FeeWorkAdapterRegistry adapterRegistry;
    private final FeeContributorService contributorService;
    private final FeeCalculationService calculationService;
    private final FeeQueryService queryService;
    private final FeeWorkStateService stateService;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, String username, String realName, Integer roleCode, FeeSubmitDTO dto) {
        String workType = dto.getWorkType();
        Map<String, Object> workInfo = dto.getWorkInfo();
        List<FeeSubmitDTO.ContributorDTO> contributors = dto.getContributors();
        accessService.ensureCanSubmit(userId, roleCode, workType);
        normalizeUnpublishedDate(workType, workInfo);
        FeeWorkAdapter adapter = adapterRegistry.require(workType);
        ensureNotDuplicate(adapter, workInfo, contributors, null);
        boolean canManageAmount = accessService.canManageAmount(roleCode);
        if (!canManageAmount) {
            workInfo.put("isSpecialNode", 0);
            workInfo.put("specialMultiplier", 1);
        }
        if (adapter.supports() == FeeWorkType.VISUAL) {
            workInfo.put("authorCount", contributors != null ? contributors.size() : 0);
        }
        Long workId = adapter.create(workInfo, userId);
        List<BigDecimal> fees = calculationService.calculateAutomaticFees(workType, workInfo, contributors);
        if (fees == null && contributors != null) {
            // 202609210900 - 保留管理员提交无法自动计价项目时使用显式金额的原有行为。
            fees = contributors.stream()
                    .map(contributor -> canManageAmount && contributor.getFeeAmount() != null
                            ? contributor.getFeeAmount() : BigDecimal.ZERO)
                    .toList();
        }
        BigDecimal total = contributorService.insert(workType, workId, contributors, fees);
        adapter.updateTotal(workId, total, contributors != null ? contributors.size() : 0);
        operationLogService.log(userId, realName, "", "CREATE", "FEE", workId,
                String.format("提交稿费申请 [%s]，贡献者: %d人", workType,
                        contributors != null ? contributors.size() : 0), "");
        log.info("稿费提交完成: type={}, id={}, contributors={}", workType, workId,
                contributors != null ? contributors.size() : 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, String workType, FeeSubmitDTO dto, Long operatorId,
                       String operatorName, String operatorRole, Integer roleCode) {
        accessService.ensureCanEdit(roleCode);
        accessService.ensureCanView(operatorId, roleCode, workType);
        if (!workType.equals(dto.getWorkType())) throw ApiException.badRequest("作品类型不能变更");
        boolean canManageAmount = accessService.canManageAmount(roleCode);
        String originalStatus = stateService.auditStatus(workType, id);
        if (!canManageAmount && !"SUBMITTED".equals(originalStatus) && !"REJECTED".equals(originalStatus)) {
            throw ApiException.badRequest("部长只能修改待审核或已驳回的稿费任务");
        }
        FeeDetailVO before = queryService.detail(id, workType, operatorId, roleCode);
        FeeWorkAdapter adapter = adapterRegistry.require(workType);
        Map<String, Object> info = canManageAmount ? dto.getWorkInfo()
                : adapter.preserveRestrictedFields(id, dto.getWorkInfo());
        normalizeUnpublishedDate(workType, info);
        ensureNotDuplicate(adapter, info, dto.getContributors(), id);
        adapter.update(id, info);
        List<BigDecimal> fees = calculationService.calculateAutomaticFees(workType, info, dto.getContributors());
        BigDecimal total = contributorService.replace(workType, id, dto.getContributors(), fees);
        adapter.updateTotal(id, total, dto.getContributors() != null ? dto.getContributors().size() : 0);
        if ("REJECTED".equals(originalStatus)) stateService.updateAudit(workType, id, "SUBMITTED", null);

        FeeDetailVO after = queryService.detail(id, workType, operatorId, roleCode);
        String detail = ("REJECTED".equals(originalStatus)
                ? String.format("修改并重新提交稿费任务 [%s] id=%d", workType, id)
                : String.format("修改未审核稿费任务 [%s] id=%d", workType, id))
                + "；变更字段：" + summarizeChanges(before, after);
        operationLogService.logChange(operatorId, operatorName, operatorRole, "UPDATE", "FEE", id,
                detail, snapshot(before), snapshot(after), "");
    }

    public List<BigDecimal> calculate(FeeSubmitDTO dto, Integer roleCode) {
        accessService.ensureCanManageAmount(roleCode, "计算并查看稿费金额");
        return calculationService.calculateAutomaticFees(dto.getWorkType(), dto.getWorkInfo(), dto.getContributors());
    }

    private void normalizeUnpublishedDate(String workType, Map<String, Object> info) {
        if (FeeWorkType.require(workType) != FeeWorkType.SUBSIDY
                && !Objects.equals(FeeValueReader.integer(info, "isPublished"), 1)) {
            info.put("publishDate", null);
        }
    }

    private void ensureNotDuplicate(FeeWorkAdapter adapter, Map<String, Object> info,
                                    List<FeeSubmitDTO.ContributorDTO> contributors, Long excludedId) {
        if (adapter.existsDuplicate(info, contributors, excludedId)) {
            throw ApiException.conflict("检测到相同作品或同月重复补贴，请勿重复提交");
        }
    }

    private String summarizeChanges(FeeDetailVO before, FeeDetailVO after) {
        List<String> changed = new ArrayList<>();
        Map<String, Object> beforeInfo = before.getWorkInfo() != null ? before.getWorkInfo() : Collections.emptyMap();
        Map<String, Object> afterInfo = after.getWorkInfo() != null ? after.getWorkInfo() : Collections.emptyMap();
        Set<String> keys = new LinkedHashSet<>(); keys.addAll(beforeInfo.keySet()); keys.addAll(afterInfo.keySet());
        for (String key : keys) {
            if (!valuesEqual(beforeInfo.get(key), afterInfo.get(key))) {
                changed.add(AUDIT_FIELDS.getOrDefault(key, key));
            }
        }
        if (!valuesEqual(before.getContributors(), after.getContributors())) changed.add("参与人员及金额");
        if (!valuesEqual(before.getTotalAmount(), after.getTotalAmount())) changed.add("总金额");
        if (!valuesEqual(before.getStatus(), after.getStatus())) changed.add("审核状态");
        if (!valuesEqual(before.getRemark(), after.getRemark())) changed.add("备注");
        return changed.isEmpty() ? "无实际变化" : String.join("、", changed);
    }

    private boolean valuesEqual(Object left, Object right) {
        if (left instanceof BigDecimal first && right instanceof BigDecimal second) {
            return first.compareTo(second) == 0;
        }
        return Objects.equals(left, right);
    }

    private String snapshot(FeeDetailVO detail) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("workType", detail.getWorkType()); snapshot.put("workInfo", detail.getWorkInfo());
        snapshot.put("contributors", detail.getContributors()); snapshot.put("status", detail.getStatus());
        snapshot.put("totalAmount", detail.getTotalAmount()); snapshot.put("remark", detail.getRemark());
        try {
            return AUDIT_MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("稿费变更快照序列化失败", exception);
        }
    }

    private static Map<String, String> createAuditFields() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("title", "标题"); labels.put("videoTitle", "视频标题"); labels.put("platform", "发布平台");
        labels.put("category", "类别"); labels.put("wordCount", "字数"); labels.put("readCount", "阅读量");
        labels.put("photoCount", "图片数量"); labels.put("videoDurationSec", "视频时长"); labels.put("likeCount", "点赞数");
        labels.put("reprintStatus", "转载情况"); labels.put("isReposted", "是否官方转载");
        labels.put("isComplexEdit", "是否复杂剪辑"); labels.put("authorCount", "参与人数");
        labels.put("authorAvgFee", "人均金额"); labels.put("publishDate", "发布时间"); labels.put("articleLink", "文章链接");
        labels.put("videoLink", "视频链接"); labels.put("isPublished", "是否已发布"); labels.put("isSpecialNode", "特殊节点");
        labels.put("specialMultiplier", "特殊节点倍数"); labels.put("subsidyMonth", "补贴月份"); labels.put("remark", "备注");
        return Collections.unmodifiableMap(labels);
    }
}
