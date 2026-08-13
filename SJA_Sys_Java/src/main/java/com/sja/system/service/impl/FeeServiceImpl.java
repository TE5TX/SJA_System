package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.common.PageResult;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeAuditDTO;
import com.sja.system.dto.FeeBatchAuditDTO;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.FeeTaskRefDTO;
import com.sja.system.entity.*;
import com.sja.system.mapper.*;
import com.sja.system.service.FeeService;
import com.sja.system.service.FeeCalculationService;
import com.sja.system.service.OperationLogService;
import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fee (remuneration) service implementation.
 * Handles CRUD for editorial, photography, video, and visual works
 * along with their contributor/fee breakdowns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeService {

    // 202608101716 - 将稿费保存、展示和导出统一为一位小数。
    private static final int MONEY_SCALE = 1;

    private static final String EDITORIAL = "EDITORIAL";
    private static final String PHOTO = "PHOTO";
    private static final String VIDEO = "VIDEO";
    private static final String VISUAL = "VISUAL";
    private static final String SUBSIDY = "SUBSIDY";
    private static final Set<String> EXPORT_TEMPLATE_TYPES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "EDITORIAL_WECHAT", "EDITORIAL_WEBSITE", "EDITORIAL_NEWSPAPER",
            "PHOTO", "VISUAL", "VIDEO", "SUBSIDY", "CUSTOM")));
    private static final Map<String, String> EXPORT_FIELD_LABELS = createExportFieldLabels();
    private static final ObjectMapper AUDIT_OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final Map<String, String> FEE_AUDIT_FIELD_LABELS = createFeeAuditFieldLabels();

    private final BizEditorialArticleMapper editorialMapper;
    private final BizPhotographyWorkMapper photographyMapper;
    private final BizVideoWorkMapper videoMapper;
    private final BizVisualWorkMapper visualMapper;
    private final BizMonthlySubsidyMapper monthlySubsidyMapper;
    private final BizWorkContributorMapper contributorMapper;
    private final BizFeeStandardMapper feeStandardMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final OperationLogService operationLogService;
    private final FeeCalculationService feeCalculationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFee(Long userId, String username, String realName, Integer roleCode, FeeSubmitDTO dto) {
        String workType = dto.getWorkType();
        Map<String, Object> workInfo = dto.getWorkInfo();
        List<FeeSubmitDTO.ContributorDTO> contributors = dto.getContributors();
        ensureCanSubmitWorkType(userId, roleCode, workType);
        normalizeUnpublishedPublishDate(workType, workInfo);
        // 202608101042 - 修复了作品和同月补贴可以重复计费的问题。
        ensureNotDuplicate(workType, workInfo, contributors, null);
        boolean canSeeAmount = canManageFeeAmount(roleCode);
        if (!canSeeAmount) {
            workInfo.put("isSpecialNode", 0);
            workInfo.put("specialMultiplier", 1);
        }
        if (VISUAL.equals(workType)) {
            workInfo.put("authorCount", contributors != null ? contributors.size() : 0);
        }

        Long workId;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 1. Create the work entity based on type
        switch (workType) {
            case EDITORIAL:
                workId = createEditorialWork(workInfo, userId);
                break;
            case PHOTO:
                workId = createPhotographyWork(workInfo, userId);
                break;
            case VIDEO:
                workId = createVideoWork(workInfo, userId);
                break;
            case VISUAL:
                workId = createVisualWork(workInfo, userId);
                break;
            case SUBSIDY:
                workId = createMonthlySubsidy(workInfo, userId);
                break;
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }

        // 2. Create contributor records
        List<BigDecimal> automaticFees = feeCalculationService.calculateAutomaticFees(workType, workInfo, contributors);
        if (contributors != null && !contributors.isEmpty()) {
            for (int index = 0; index < contributors.size(); index++) {
                FeeSubmitDTO.ContributorDTO c = contributors.get(index);
                BizWorkContributor contributor = new BizWorkContributor();
                contributor.setWorkType(workType);
                contributor.setWorkId(workId);
                contributor.setUserId(c.getUserId());
                contributor.setRoleInWork((EDITORIAL.equals(workType) || PHOTO.equals(workType)) ? "AUTHOR" : c.getRoleInWork());
                BigDecimal feeAmount = automaticFees != null
                        ? automaticFees.get(index)
                        : (canSeeAmount && c.getFeeAmount() != null ? c.getFeeAmount() : BigDecimal.ZERO);
                contributor.setFeeAmount(normalizeMoney(feeAmount));
                contributor.setWorkCount(c.getWorkCount());
                contributor.setRemark(c.getRemark());
                contributorMapper.insert(contributor);

                totalAmount = totalAmount.add(contributor.getFeeAmount());
            }
        }

        // 3. Update total amount on the work entity
        updateWorkTotalAmount(workType, workId, totalAmount, contributors != null ? contributors.size() : 0);

        // 4. Log operation
        operationLogService.log(userId, realName, "", "CREATE",
                "FEE", workId,
                String.format("提交稿费申请 [%s]，贡献者: %d人",
                        workType, contributors != null ? contributors.size() : 0),
                "");

        log.info("Fee submitted: type={}, id={}, contributors={}",
                workType, workId, contributors != null ? contributors.size() : 0);
    }

    @Override
    public PageResult<FeeListVO> getFeeList(String deptCode, String month, String status, String contributorName,
                                            Integer pageNum, Integer pageSize,
                                            Long operatorId, Integer roleCode) {
        int pn = pageNum != null ? Math.max(1, pageNum) : 1;
        // 202608111533 - 修复了稿费审核列表单页最多只能显示50条且服务端未限制分页大小的问题。
        int ps = pageSize != null ? Math.min(200, Math.max(1, pageSize)) : 10;

        List<FeeListVO> allItems = new ArrayList<>();

        // Query each work type and combine results
        allItems.addAll(queryEditorialList(month));
        allItems.addAll(queryPhotographyList(month));
        allItems.addAll(queryVideoList(month));
        allItems.addAll(queryVisualList(month));
        allItems.addAll(queryMonthlySubsidyList(month));

        String selectedDeptCode = feeDeptFilterBase(deptCode);
        String selectedPlatform = feePlatformFilter(deptCode);
        Set<String> allowedDeptCodes = allowedFeeDeptCodes(operatorId, roleCode);
        if (StringUtils.hasText(deptCode)) {
            if (!allowedDeptCodes.contains(selectedDeptCode)) {
                return new PageResult<>(Collections.emptyList(), 0L, (long) pn, (long) ps);
            }
            allowedDeptCodes = Collections.singleton(selectedDeptCode);
        }
        Set<String> finalAllowedDeptCodes = allowedDeptCodes;
        allItems = allItems.stream()
                .filter(item -> finalAllowedDeptCodes.contains(workTypeToDeptCode(item.getWorkType())))
                .filter(item -> !StringUtils.hasText(selectedPlatform) || selectedPlatform.equals(item.getPlatform()))
                .collect(Collectors.toList());

        if (StringUtils.hasText(status)) {
            allItems = allItems.stream()
                    .filter(item -> status.equals(item.getStatus()))
                    .collect(Collectors.toList());
        }

        // 202608121041 - 新增了按参与人员姓名查找稿费任务的功能。
        if (StringUtils.hasText(contributorName)) {
            Set<String> matchedTasks = findTasksByContributorName(contributorName);
            allItems = allItems.stream()
                    .filter(item -> matchedTasks.contains(feeTaskKey(item.getWorkType(), item.getId())))
                    .collect(Collectors.toList());
        }

        if (!canManageFeeAmount(roleCode)) {
            allItems.forEach(item -> item.setTotalAmount(null));
        }

        // 202608121041 - 修复了稿费审核列表未按发布日期由近到远排列的问题。
        allItems.sort(Comparator
                .comparing(FeeListVO::getPublishDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FeeListVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FeeListVO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        // Manual pagination
        long total = allItems.size();
        int fromIndex = (pn - 1) * ps;
        int toIndex = Math.min(fromIndex + ps, allItems.size());
        List<FeeListVO> pagedList = fromIndex < allItems.size()
                ? allItems.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return new PageResult<>(pagedList, total, (long) pn, (long) ps);
    }

    @Override
    public FeeDetailVO getFeeDetail(Long id, String workType, Long operatorId, Integer roleCode) {
        ensureCanViewWorkType(operatorId, roleCode, workType);
        boolean canSeeAmount = canManageFeeAmount(roleCode);
        FeeDetailVO vo = new FeeDetailVO();
        vo.setId(id);
        vo.setWorkType(workType);

        Map<String, Object> workInfo = new LinkedHashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        switch (workType) {
            case EDITORIAL: {
                BizEditorialArticle entity = editorialMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("稿件不存在或已失效");
                workInfo.put("title", entity.getTitle());
                workInfo.put("platform", entity.getPlatform());
                workInfo.put("category", entity.getCategory());
                if ("微信".equals(entity.getPlatform())) {
                    workInfo.put("readCount", entity.getReadCount());
                } else if ("校报".equals(entity.getPlatform())) {
                    workInfo.put("wordCount", entity.getWordCount());
                }
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("articleLink", entity.getArticleLink());
                workInfo.put("isPublished", entity.getIsPublished());
                if (canSeeAmount) {
                    workInfo.put("isSpecialNode", entity.getIsSpecialNode());
                    workInfo.put("specialMultiplier", entity.getSpecialMultiplier());
                }
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                vo.setRemark(entity.getRemark());
                break;
            }
            case PHOTO: {
                BizPhotographyWork entity = photographyMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("摄影作品不存在或已失效");
                workInfo.put("title", entity.getTitle());
                workInfo.put("platform", entity.getPlatform());
                if ("微信".equals(entity.getPlatform())) {
                    workInfo.put("readCount", entity.getReadCount());
                    workInfo.put("category", entity.getCategory());
                }
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("articleLink", entity.getArticleLink());
                workInfo.put("isPublished", entity.getIsPublished());
                if (canSeeAmount) {
                    workInfo.put("isSpecialNode", entity.getIsSpecialNode());
                    workInfo.put("specialMultiplier", entity.getSpecialMultiplier());
                }
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                vo.setRemark(entity.getRemark());
                break;
            }
            case VIDEO: {
                BizVideoWork entity = videoMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("视频作品不存在或已失效");
                workInfo.put("videoTitle", entity.getVideoTitle());
                workInfo.put("category", entity.getCategory());
                workInfo.put("videoDurationSec", entity.getVideoDurationSec());
                workInfo.put("likeCount", entity.getLikeCount());
                workInfo.put("isReposted", "官方媒体转载".equals(entity.getReprintStatus()) ? 1 : 0);
                workInfo.put("isComplexEdit", entity.getIsComplexEdit());
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("videoLink", entity.getVideoLink());
                workInfo.put("isPublished", entity.getIsPublished());
                if (canSeeAmount) {
                    workInfo.put("isSpecialNode", entity.getIsSpecialNode());
                    workInfo.put("specialMultiplier", entity.getSpecialMultiplier());
                }
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                vo.setRemark(entity.getRemark());
                break;
            }
            case VISUAL: {
                BizVisualWork entity = visualMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("视觉作品不存在或已失效");
                workInfo.put("title", entity.getTitle());
                workInfo.put("category", entity.getCategory());
                workInfo.put("authorCount", entity.getAuthorCount());
                if (canSeeAmount) {
                    workInfo.put("authorAvgFee", entity.getAuthorAvgFee());
                }
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("articleLink", entity.getArticleLink());
                workInfo.put("isPublished", entity.getIsPublished());
                if (canSeeAmount) {
                    workInfo.put("isSpecialNode", entity.getIsSpecialNode());
                    workInfo.put("specialMultiplier", entity.getSpecialMultiplier());
                }
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                vo.setRemark(entity.getRemark());
                break;
            }
            case SUBSIDY: {
                BizMonthlySubsidy entity = monthlySubsidyMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("月度补贴任务不存在或已失效");
                workInfo.put("title", entity.getTitle());
                workInfo.put("category", entity.getCategory());
                workInfo.put("subsidyMonth", entity.getSubsidyMonth());
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                vo.setRemark(entity.getRemark());
                break;
            }
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }

        vo.setWorkInfo(workInfo);
        vo.setTotalAmount(canSeeAmount ? totalAmount : null);

        // Load contributors
        QueryWrapper<BizWorkContributor> cQuery = new QueryWrapper<>();
        cQuery.eq("work_type", workType).eq("work_id", id);
        List<BizWorkContributor> contributors = contributorMapper.selectList(cQuery);

        List<FeeDetailVO.ContributorVO> contributorVOs = contributors.stream().map(c -> {
            FeeDetailVO.ContributorVO cvo = new FeeDetailVO.ContributorVO();
            cvo.setUserId(c.getUserId());
            cvo.setRoleInWork(c.getRoleInWork());
            cvo.setFeeAmount(canSeeAmount ? c.getFeeAmount() : null);
            cvo.setWorkCount(c.getWorkCount());

            // Look up real name
            SysUser user = sysUserMapper.selectById(c.getUserId());
            cvo.setRealName(user != null ? user.getRealName() : "未知");
            cvo.setStudentNo(user != null ? user.getStudentNo() : "");

            return cvo;
        }).collect(Collectors.toList());

        vo.setContributors(contributorVOs);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFee(Long id, String workType, FeeSubmitDTO dto, Long operatorId,
                          String operatorName, String operatorRole, Integer roleCode) {
        if (!canEditFeeTask(roleCode)) {
            throw ApiException.forbidden("没有权限修改稿费任务");
        }
        ensureCanViewWorkType(operatorId, roleCode, workType);
        if (!workType.equals(dto.getWorkType())) {
            throw ApiException.badRequest("作品类型不能变更");
        }

        boolean canManageAmount = canManageFeeAmount(roleCode);
        String originalStatus = getAuditStatus(workType, id);
        if (!canManageAmount && !"SUBMITTED".equals(originalStatus) && !"REJECTED".equals(originalStatus)) {
            throw ApiException.badRequest("部长只能修改待审核或已驳回的稿费任务");
        }

        FeeDetailVO before = getFeeDetail(id, workType, operatorId, roleCode);

        Map<String, Object> workInfo = canManageAmount
                ? dto.getWorkInfo()
                : preserveRestrictedFeeFields(workType, id, dto.getWorkInfo());

        normalizeUnpublishedPublishDate(workType, workInfo);
        ensureNotDuplicate(workType, workInfo, dto.getContributors(), id);

        updateWorkInfo(workType, id, workInfo);

        QueryWrapper<BizWorkContributor> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("work_type", workType).eq("work_id", id);
        contributorMapper.delete(deleteQuery);

        // 202608121041 - 修复了管理员或秘书长编辑采编、摄影、视觉任务后仍保存旧个人稿费的问题。
        List<BigDecimal> automaticFees = feeCalculationService.calculateAutomaticFees(
                workType, workInfo, dto.getContributors());
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (dto.getContributors() != null) {
            for (int index = 0; index < dto.getContributors().size(); index++) {
                FeeSubmitDTO.ContributorDTO c = dto.getContributors().get(index);
                BizWorkContributor contributor = new BizWorkContributor();
                contributor.setWorkType(workType);
                contributor.setWorkId(id);
                contributor.setUserId(c.getUserId());
                contributor.setRoleInWork((EDITORIAL.equals(workType) || PHOTO.equals(workType)) ? "AUTHOR" : c.getRoleInWork());
                contributor.setFeeAmount(normalizeMoney(automaticFees.get(index)));
                contributor.setWorkCount(c.getWorkCount());
                contributor.setRemark(c.getRemark());
                contributorMapper.insert(contributor);
                totalAmount = totalAmount.add(contributor.getFeeAmount());
            }
        }
        updateWorkTotalAmount(workType, id, totalAmount,
                dto.getContributors() != null ? dto.getContributors().size() : 0);
        if ("REJECTED".equals(originalStatus)) {
            updateAuditStatus(workType, id, "SUBMITTED");
        }

        FeeDetailVO after = getFeeDetail(id, workType, operatorId, roleCode);
        String detail = "REJECTED".equals(originalStatus)
                ? String.format("修改并重新提交稿费任务 [%s] id=%d", workType, id)
                : String.format("修改未审核稿费任务 [%s] id=%d", workType, id);
        detail += "；变更字段：" + summarizeFeeChanges(before, after);
        operationLogService.logChange(operatorId, operatorName, operatorRole, "UPDATE", "FEE", id,
                detail, feeAuditSnapshot(before), feeAuditSnapshot(after), "");
    }

    @Override
    public List<BigDecimal> calculateFee(FeeSubmitDTO dto) {
        return feeCalculationService.calculateAutomaticFees(
                dto.getWorkType(), dto.getWorkInfo(), dto.getContributors());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditFee(Long id, String workType, FeeAuditDTO dto, Long auditorId,
                         String auditorName, String auditorRole) {
        if (!"SUBMITTED".equals(getAuditStatus(workType, id))) {
            throw ApiException.badRequest("只有待审核的稿费任务可以审核");
        }
        String action = Boolean.TRUE.equals(dto.getApproved()) ? "审核通过" : "审核驳回";
        String opinion = dto.getOpinion() != null ? dto.getOpinion() : "";

        // Store audit result in the remark field of the work entity
        String auditRemark = String.format("[%s] %s: %s | %s",
                auditorName, action, opinion,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        switch (workType) {
            case EDITORIAL: {
                BizEditorialArticle entity = editorialMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("稿件不存在或已失效");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizEditorialArticle update = new BizEditorialArticle();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                update.setAuditStatus(Boolean.TRUE.equals(dto.getApproved()) ? "APPROVED" : "REJECTED");
                editorialMapper.updateById(update);
                break;
            }
            case PHOTO: {
                BizPhotographyWork entity = photographyMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("摄影作品不存在或已失效");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizPhotographyWork update = new BizPhotographyWork();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                update.setAuditStatus(Boolean.TRUE.equals(dto.getApproved()) ? "APPROVED" : "REJECTED");
                photographyMapper.updateById(update);
                break;
            }
            case VIDEO: {
                BizVideoWork entity = videoMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("视频作品不存在或已失效");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizVideoWork update = new BizVideoWork();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                update.setAuditStatus(Boolean.TRUE.equals(dto.getApproved()) ? "APPROVED" : "REJECTED");
                videoMapper.updateById(update);
                break;
            }
            case VISUAL: {
                BizVisualWork entity = visualMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("视觉作品不存在或已失效");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizVisualWork update = new BizVisualWork();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                update.setAuditStatus(Boolean.TRUE.equals(dto.getApproved()) ? "APPROVED" : "REJECTED");
                visualMapper.updateById(update);
                break;
            }
            case SUBSIDY: {
                BizMonthlySubsidy entity = monthlySubsidyMapper.selectById(id);
                if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("月度补贴任务不存在或已失效");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizMonthlySubsidy update = new BizMonthlySubsidy();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                update.setAuditStatus(Boolean.TRUE.equals(dto.getApproved()) ? "APPROVED" : "REJECTED");
                monthlySubsidyMapper.updateById(update);
                break;
            }
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }

        operationLogService.log(auditorId, auditorName, auditorRole, "AUDIT",
                "FEE", id, String.format("%s [%s] id=%d: %s", action, workType, id, opinion), "");

        log.info("Fee audited: type={}, id={}, approved={}, by={}", workType, id, dto.getApproved(), auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAuditFee(FeeBatchAuditDTO dto, Long auditorId,
                              String auditorName, String auditorRole) {
        Set<String> taskKeys = dto.getTasks().stream()
                .map(this::feeTaskKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (taskKeys.size() != dto.getTasks().size()) {
            throw ApiException.badRequest("批量审核任务不能重复");
        }
        FeeAuditDTO auditDTO = new FeeAuditDTO();
        auditDTO.setApproved(dto.getApproved());
        auditDTO.setOpinion(dto.getOpinion());
        // 202608111533 - 修复了稿费任务无法批量通过或批量驳回的问题。
        for (FeeTaskRefDTO task : dto.getTasks()) {
            auditFee(task.getId(), task.getWorkType(), auditDTO, auditorId, auditorName, auditorRole);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetAudit(Long id, String workType, Long operatorId,
                           String operatorName, String operatorRole) {
        if (!"APPROVED".equals(getAuditStatus(workType, id))) {
            throw ApiException.badRequest("只有已通过的稿费任务可以退回待审核");
        }
        updateAuditStatus(workType, id, "SUBMITTED");
        appendWorkRemark(workType, id, String.format("[%s] 退回待审核 | %s",
                operatorName, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        operationLogService.log(operatorId, operatorName, operatorRole, "RESET_AUDIT",
                "FEE", id, String.format("将已通过稿费任务退回待审核 [%s] id=%d", workType, id), "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidateRejectedFee(Long id, String workType, Long operatorId,
                                      String operatorName, String operatorRole, Integer roleCode) {
        if (!canEditFeeTask(roleCode)) {
            throw ApiException.forbidden("没有权限删除稿费任务");
        }
        ensureCanViewWorkType(operatorId, roleCode, workType);
        if (!"REJECTED".equals(getAuditStatus(workType, id))) {
            throw ApiException.badRequest("只有已驳回的稿费任务可以删除");
        }

        updateWorkValidity(workType, id, 0);
        operationLogService.log(operatorId, operatorName, operatorRole, "DELETE",
                "FEE", id, String.format("将已驳回稿费任务设为无效 [%s] id=%d", workType, id), "");
    }

    @Override
    public void exportFee(FeeExportDTO dto, Long operatorId, Integer roleCode, HttpServletResponse response) {
        if (!canManageFeeAmount(roleCode)) {
            throw ApiException.forbidden("只有管理员或秘书长可以导出稿费表");
        }
        List<String> fields = dto.getFields().stream().distinct().collect(Collectors.toList());
        if (fields.size() != dto.getFields().size() || !EXPORT_FIELD_LABELS.keySet().containsAll(fields)) {
            throw ApiException.badRequest("包含重复或不支持的导出字段");
        }
        String templateType = StringUtils.hasText(dto.getTemplateType()) ? dto.getTemplateType() : "CUSTOM";
        if (!EXPORT_TEMPLATE_TYPES.contains(templateType)) {
            throw ApiException.badRequest("不支持的稿费导出模板");
        }
        boolean fixedTemplate = FeeTemplateExportWriter.supports(templateType);
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String exportName = fixedTemplate ? templateExportFileName(templateType) : "稿费清单";
            String fileName = URLEncoder_encode(exportName + "_" + (dto.getMonth() != null ? dto.getMonth() : "全部") + ".xlsx");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            List<FeeListVO> allItems = new ArrayList<>();
            allItems.addAll(queryEditorialList(dto.getMonth()));
            allItems.addAll(queryPhotographyList(dto.getMonth()));
            allItems.addAll(queryVideoList(dto.getMonth()));
            allItems.addAll(queryVisualList(dto.getMonth()));
            allItems.addAll(queryMonthlySubsidyList(dto.getMonth()));
            String selectedDeptCode = feeDeptFilterBase(dto.getDeptCode());
            String selectedPlatform = feePlatformFilter(dto.getDeptCode());
            Set<String> allowedDeptCodes = allowedFeeDeptCodes(operatorId, roleCode);
            if (StringUtils.hasText(dto.getDeptCode())) {
                if (!allowedDeptCodes.contains(selectedDeptCode)) {
                    throw ApiException.forbidden("不能导出该部门的稿费任务");
                }
                allowedDeptCodes = Collections.singleton(selectedDeptCode);
            }
            Set<String> finalAllowedDeptCodes = allowedDeptCodes;
            allItems = allItems.stream()
                    .filter(item -> finalAllowedDeptCodes.contains(workTypeToDeptCode(item.getWorkType())))
                    .filter(item -> !StringUtils.hasText(selectedPlatform) || selectedPlatform.equals(item.getPlatform()))
                    .filter(item -> !StringUtils.hasText(dto.getStatus()) || dto.getStatus().equals(item.getStatus()))
                    .collect(Collectors.toList());
            if (StringUtils.hasText(dto.getContributorName())) {
                Set<String> matchedTasks = findTasksByContributorName(dto.getContributorName());
                allItems = allItems.stream()
                        .filter(item -> matchedTasks.contains(feeTaskKey(item.getWorkType(), item.getId())))
                        .collect(Collectors.toList());
            }

            if (fixedTemplate) {
                allItems = allItems.stream()
                        .filter(item -> FeeTemplateExportWriter.matches(templateType, item))
                        .collect(Collectors.toList());
            }

            if (dto.getSelectedTasks() != null && !dto.getSelectedTasks().isEmpty()) {
                Set<String> selectedTaskKeys = dto.getSelectedTasks().stream()
                        .map(this::feeTaskKey)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (selectedTaskKeys.size() != dto.getSelectedTasks().size()) {
                    throw ApiException.badRequest("批量导出任务不能重复");
                }
                allItems = allItems.stream()
                        .filter(item -> selectedTaskKeys.contains(feeTaskKey(item.getWorkType(), item.getId())))
                        .collect(Collectors.toList());
                if (allItems.size() != selectedTaskKeys.size()) {
                    throw ApiException.badRequest(fixedTemplate
                            ? "部分选中任务与当前导出模板不匹配"
                            : "部分选中任务不存在、已失效或无权导出");
                }
                // 202608111533 - 修复了稿费审核页不能只导出勾选任务的问题。
            }

            allItems.sort(Comparator
                    .comparing(FeeListVO::getPublishDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(FeeListVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(FeeListVO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

            if (fixedTemplate) {
                List<FeeTemplateExportWriter.ExportItem> exportItems = new ArrayList<>();
                for (FeeListVO item : allItems) {
                    exportItems.add(new FeeTemplateExportWriter.ExportItem(item,
                            getFeeDetail(item.getId(), item.getWorkType(), operatorId, roleCode)));
                }
                FeeTemplateExportWriter.write(response.getOutputStream(), templateType, exportItems, dto.getMonth());
                // 202608131708 - 将稿费审核页的微信、院网、摄影、视觉和视频导出改为现有部门工作表模板版式。
            } else {
                List<List<String>> head = fields.stream()
                        .map(field -> Collections.singletonList(EXPORT_FIELD_LABELS.get(field)))
                        .collect(Collectors.toList());
                // 202608101042 - 修复了稿费导出不符合分部门工作表习惯的问题，并设置自适应列宽和分表合计。
                Map<String, List<FeeListVO>> grouped = allItems.stream()
                        .collect(Collectors.groupingBy(FeeListVO::getWorkType, LinkedHashMap::new, Collectors.toList()));
                try (ExcelWriter writer = EasyExcel.write(response.getOutputStream())
                        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                        .build()) {
                    int sheetIndex = 0;
                    for (String type : Arrays.asList(EDITORIAL, PHOTO, VIDEO, VISUAL, SUBSIDY)) {
                        List<FeeListVO> items = grouped.get(type);
                        if (items == null || items.isEmpty()) continue;
                        List<List<String>> data = buildExportRows(items, fields, operatorId, roleCode);
                        WriteSheet sheet = EasyExcel.writerSheet(sheetIndex++, exportSheetName(type))
                                .head(head).build();
                        writer.write(data, sheet);
                    }
                    if (sheetIndex == 0) {
                        writer.write(Collections.emptyList(), EasyExcel.writerSheet(0, "稿费明细").head(head).build());
                    }
                }
            }

            operationLogService.log(operatorId, "用户" + operatorId, String.valueOf(roleCode), "EXPORT",
                    "FEE", null, String.format("导出稿费明细 %d 条，模板：%s，字段：%s",
                            allItems.size(), templateType, fields), "");

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to export fee data", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    // ======================== Private helpers ========================

    private String templateExportFileName(String templateType) {
        switch (templateType) {
            case "EDITORIAL_WECHAT": return "校学生记者团稿费统计（微信）";
            case "EDITORIAL_WEBSITE": return "校学生记者团稿费统计（院网）";
            case "PHOTO": return "校学生记者团稿费统计（摄影）";
            case "VISUAL": return "校学生记者团稿费统计（视觉）";
            case "VIDEO": return "校学生记者团稿费统计（视频）";
            default: return "稿费清单";
        }
    }

    private static Map<String, String> createExportFieldLabels() {
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

    private String exportFieldValue(String field, FeeListVO item, FeeDetailVO detail) {
        Map<String, Object> info = detail.getWorkInfo() != null ? detail.getWorkInfo() : Collections.emptyMap();
        List<FeeDetailVO.ContributorVO> contributors = detail.getContributors() != null
                ? detail.getContributors() : Collections.emptyList();
        switch (field) {
            case "workType": return workTypeCategoryDisplayName(item.getWorkType());
            case "deptName": return item.getDeptName();
            case "title": return item.getTitle();
            case "platform": return displayValue(info.get("platform"));
            case "category": return displayValue(info.get("category"));
            case "publishDate": return displayValue(info.get("publishDate"));
            case "articleLink": return displayValue(info.containsKey("videoLink") ? info.get("videoLink") : info.get("articleLink"));
            case "isPublished": return yesNo(info.get("isPublished"));
            case "readCount": return displayValue(info.get("readCount"));
            case "wordCount": return displayValue(info.get("wordCount"));
            case "videoDurationSec": return displayValue(info.get("videoDurationSec"));
            case "likeCount": return displayValue(info.get("likeCount"));
            case "isReposted": return yesNo(info.get("isReposted"));
            case "isComplexEdit": return yesNo(info.get("isComplexEdit"));
            case "subsidyMonth": return displayValue(info.get("subsidyMonth"));
            case "contributorNames": return joinContributorValues(contributors, FeeDetailVO.ContributorVO::getRealName);
            case "contributorStudentNos": return joinContributorValues(contributors, FeeDetailVO.ContributorVO::getStudentNo);
            case "contributorRoles": return joinContributorValues(contributors, c -> contributorRoleDisplayName(c.getRoleInWork()));
            case "workCounts": return joinContributorValues(contributors, c -> c.getWorkCount() != null ? String.valueOf(c.getWorkCount()) : "");
            case "contributorAmounts": return joinContributorValues(contributors, c -> formatMoney(c.getFeeAmount()));
            case "totalAmount": return formatMoney(detail.getTotalAmount());
            case "auditStatus": return auditStatusDisplayName(detail.getStatus());
            case "isSpecialNode": return yesNo(info.get("isSpecialNode"));
            case "specialMultiplier": return displayValue(info.get("specialMultiplier"));
            case "remark": return displayValue(detail.getRemark());
            case "createTime": return item.getCreateTime() != null
                    ? item.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
            default: throw ApiException.badRequest("不支持的导出字段: " + field);
        }
    }

    private String joinContributorValues(List<FeeDetailVO.ContributorVO> contributors,
                                         java.util.function.Function<FeeDetailVO.ContributorVO, String> extractor) {
        return contributors.stream().map(extractor).map(this::displayValue)
                .filter(StringUtils::hasText).collect(Collectors.joining("、"));
    }

    private List<List<String>> buildExportRows(List<FeeListVO> items, List<String> fields,
                                                Long operatorId, Integer roleCode) {
        List<List<String>> data = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (FeeListVO item : items) {
            FeeDetailVO detail = getFeeDetail(item.getId(), item.getWorkType(), operatorId, roleCode);
            data.add(fields.stream().map(field -> exportFieldValue(field, item, detail)).collect(Collectors.toList()));
            if (detail.getTotalAmount() != null) grandTotal = grandTotal.add(detail.getTotalAmount());
        }
        List<String> totalRow = new ArrayList<>(Collections.nCopies(fields.size(), ""));
        int labelIndex = fields.indexOf("title");
        if (labelIndex < 0) labelIndex = 0;
        totalRow.set(labelIndex, "合计");
        int amountIndex = fields.indexOf("totalAmount");
        if (amountIndex >= 0) totalRow.set(amountIndex, formatMoney(grandTotal));
        data.add(totalRow);
        return data;
    }

    private String exportSheetName(String workType) {
        switch (workType) {
            case EDITORIAL: return "采编部";
            case PHOTO: return "摄影部";
            case VIDEO: return "视频部";
            case VISUAL: return "视觉部";
            case SUBSIDY: return "主席团与小队";
            default: return "稿费明细";
        }
    }

    private String workTypeCategoryDisplayName(String workType) {
        switch (workType) {
            case EDITORIAL: return "采编稿件";
            case PHOTO: return "摄影作品";
            case VIDEO: return "视频作品";
            case VISUAL: return "视觉作品";
            case SUBSIDY: return "月度补贴";
            default: return workType;
        }
    }

    private String displayValue(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String feeTaskKey(FeeTaskRefDTO task) {
        return feeTaskKey(task.getWorkType(), task.getId());
    }

    private String feeTaskKey(String workType, Long id) {
        return workType + ":" + id;
    }

    private String yesNo(Object value) {
        return value == null ? "" : (Objects.equals(getIntValue(value), 1) ? "是" : "否");
    }

    private Integer getIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String contributorRoleDisplayName(String role) {
        if ("AUTHOR".equals(role)) return "作者";
        if ("SHOOTER".equals(role)) return "拍摄";
        if ("EDITOR".equals(role)) return "编辑/剪辑";
        if ("DESIGNER".equals(role)) return "设计";
        if ("MEMBER".equals(role)) return "补贴人员";
        return displayValue(role);
    }

    private String auditStatusDisplayName(String status) {
        if ("APPROVED".equals(status)) return "已通过";
        if ("REJECTED".equals(status)) return "已驳回";
        return "待审核";
    }

    private void appendWorkRemark(String workType, Long workId, String appendedRemark) {
        switch (workType) {
            case EDITORIAL: {
                BizEditorialArticle entity = editorialMapper.selectById(workId);
                BizEditorialArticle update = new BizEditorialArticle();
                update.setId(workId);
                update.setRemark(appendRemark(entity.getRemark(), appendedRemark));
                editorialMapper.updateById(update);
                break;
            }
            case PHOTO: {
                BizPhotographyWork entity = photographyMapper.selectById(workId);
                BizPhotographyWork update = new BizPhotographyWork();
                update.setId(workId);
                update.setRemark(appendRemark(entity.getRemark(), appendedRemark));
                photographyMapper.updateById(update);
                break;
            }
            case VIDEO: {
                BizVideoWork entity = videoMapper.selectById(workId);
                BizVideoWork update = new BizVideoWork();
                update.setId(workId);
                update.setRemark(appendRemark(entity.getRemark(), appendedRemark));
                videoMapper.updateById(update);
                break;
            }
            case VISUAL: {
                BizVisualWork entity = visualMapper.selectById(workId);
                BizVisualWork update = new BizVisualWork();
                update.setId(workId);
                update.setRemark(appendRemark(entity.getRemark(), appendedRemark));
                visualMapper.updateById(update);
                break;
            }
            case SUBSIDY: {
                BizMonthlySubsidy entity = monthlySubsidyMapper.selectById(workId);
                BizMonthlySubsidy update = new BizMonthlySubsidy();
                update.setId(workId);
                update.setRemark(appendRemark(entity.getRemark(), appendedRemark));
                monthlySubsidyMapper.updateById(update);
                break;
            }
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }
    }

    private String appendRemark(String existing, String appended) {
        return StringUtils.hasText(existing) ? existing + "\n" + appended : appended;
    }

    private Long createEditorialWork(Map<String, Object> info, Long submitterId) {
        BizEditorialArticle entity = new BizEditorialArticle();
        // 202608101042 - 修复了把第一位参与者误显示为提交人的问题，改为保存真实操作人。
        entity.setSubmitterId(submitterId);
        entity.setTitle(getStringVal(info, "title"));
        entity.setPlatform(getStringVal(info, "platform"));
        entity.setCategory(getStringVal(info, "category"));
        entity.setWordCount(getIntVal(info, "wordCount"));
        entity.setReadCount(getIntVal(info, "readCount"));
        entity.setPublishDate(getLocalDateVal(info, "publishDate"));
        entity.setArticleLink(getStringVal(info, "articleLink"));
        entity.setIsPublished(getIntVal(info, "isPublished"));
        entity.setIsSpecialNode(getIntVal(info, "isSpecialNode"));
        entity.setSpecialMultiplier(normalizeSpecialMultiplier(info));
        entity.setRemark(getStringVal(info, "remark"));
        editorialMapper.insert(entity);
        return entity.getId();
    }

    private Long createPhotographyWork(Map<String, Object> info, Long submitterId) {
        BizPhotographyWork entity = new BizPhotographyWork();
        entity.setSubmitterId(submitterId);
        entity.setTitle(getStringVal(info, "title"));
        entity.setPlatform(getStringVal(info, "platform"));
        entity.setCategory(getStringVal(info, "category"));
        entity.setPhotoCount(getIntVal(info, "photoCount"));
        entity.setReadCount(getIntVal(info, "readCount"));
        entity.setPublishDate(getLocalDateVal(info, "publishDate"));
        entity.setArticleLink(getStringVal(info, "articleLink"));
        entity.setIsPublished(getIntVal(info, "isPublished"));
        entity.setIsSpecialNode(getIntVal(info, "isSpecialNode"));
        entity.setSpecialMultiplier(normalizeSpecialMultiplier(info));
        entity.setRemark(getStringVal(info, "remark"));
        photographyMapper.insert(entity);
        return entity.getId();
    }

    private Long createVideoWork(Map<String, Object> info, Long submitterId) {
        BizVideoWork entity = new BizVideoWork();
        entity.setSubmitterId(submitterId);
        entity.setVideoTitle(getStringVal(info, "videoTitle"));
        entity.setCategory(getStringVal(info, "category"));
        entity.setVideoDurationSec(getIntVal(info, "videoDurationSec"));
        entity.setLikeCount(getIntVal(info, "likeCount"));
        entity.setReprintStatus(Objects.equals(getIntVal(info, "isReposted"), 1) ? "官方媒体转载" : "无");
        entity.setIsComplexEdit(getIntVal(info, "isComplexEdit"));
        entity.setPublishDate(getLocalDateVal(info, "publishDate"));
        entity.setVideoLink(getStringVal(info, "videoLink"));
        entity.setIsPublished(getIntVal(info, "isPublished"));
        entity.setIsSpecialNode(getIntVal(info, "isSpecialNode"));
        entity.setSpecialMultiplier(normalizeSpecialMultiplier(info));
        entity.setRemark(getStringVal(info, "remark"));
        videoMapper.insert(entity);
        return entity.getId();
    }

    private Long createVisualWork(Map<String, Object> info, Long submitterId) {
        BizVisualWork entity = new BizVisualWork();
        entity.setSubmitterId(submitterId);
        entity.setTitle(getStringVal(info, "title"));
        entity.setCategory(canonicalVisualCategory(getStringVal(info, "category")));
        entity.setAuthorCount(getIntVal(info, "authorCount"));
        entity.setPublishDate(getLocalDateVal(info, "publishDate"));
        entity.setArticleLink(getStringVal(info, "articleLink"));
        entity.setIsPublished(getIntVal(info, "isPublished"));
        entity.setIsSpecialNode(getIntVal(info, "isSpecialNode"));
        entity.setSpecialMultiplier(normalizeSpecialMultiplier(info));
        entity.setRemark(getStringVal(info, "remark"));
        visualMapper.insert(entity);
        return entity.getId();
    }

    private Long createMonthlySubsidy(Map<String, Object> info, Long submitterId) {
        BizMonthlySubsidy entity = new BizMonthlySubsidy();
        entity.setSubmitterId(submitterId);
        entity.setTitle(getStringVal(info, "title"));
        entity.setCategory(getStringVal(info, "category"));
        entity.setSubsidyMonth(getStringVal(info, "subsidyMonth"));
        entity.setRemark(getStringVal(info, "remark"));
        monthlySubsidyMapper.insert(entity);
        return entity.getId();
    }

    private void updateWorkTotalAmount(String workType, Long workId, BigDecimal totalAmount, int contributorCount) {
        switch (workType) {
            case EDITORIAL: {
                BizEditorialArticle update = new BizEditorialArticle();
                update.setId(workId);
                update.setTotalAmount(totalAmount);
                editorialMapper.updateById(update);
                break;
            }
            case PHOTO: {
                BizPhotographyWork update = new BizPhotographyWork();
                update.setId(workId);
                update.setTotalAmount(totalAmount);
                photographyMapper.updateById(update);
                break;
            }
            case VIDEO: {
                BizVideoWork update = new BizVideoWork();
                update.setId(workId);
                update.setTotalAmount(totalAmount);
                videoMapper.updateById(update);
                break;
            }
            case VISUAL: {
                BizVisualWork update = new BizVisualWork();
                update.setId(workId);
                update.setTotalAmount(totalAmount);
                update.setAuthorCount(contributorCount);
                // 202608101042 - 修复了视觉稿件人均金额显示为0的问题。
                update.setAuthorAvgFee(contributorCount > 0
                        ? totalAmount.divide(BigDecimal.valueOf(contributorCount), MONEY_SCALE, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
                visualMapper.updateById(update);
                break;
            }
            case SUBSIDY: {
                BizMonthlySubsidy update = new BizMonthlySubsidy();
                update.setId(workId);
                update.setTotalAmount(totalAmount);
                monthlySubsidyMapper.updateById(update);
                break;
            }
        }
    }

    private void updateWorkInfo(String workType, Long workId, Map<String, Object> info) {
        switch (workType) {
            case EDITORIAL: {
                if (editorialMapper.selectById(workId) == null) throw ApiException.notFound("稿件不存在");
                BizEditorialArticle update = new BizEditorialArticle();
                update.setId(workId);
                update.setTitle(getStringVal(info, "title"));
                update.setPlatform(getStringVal(info, "platform"));
                update.setCategory(getStringVal(info, "category"));
                update.setWordCount(getIntVal(info, "wordCount"));
                update.setReadCount(getIntVal(info, "readCount"));
                update.setPublishDate(getLocalDateVal(info, "publishDate"));
                update.setArticleLink(getStringVal(info, "articleLink"));
                update.setIsPublished(getIntVal(info, "isPublished"));
                update.setIsSpecialNode(getIntVal(info, "isSpecialNode"));
                update.setSpecialMultiplier(normalizeSpecialMultiplier(info));
                update.setRemark(getStringVal(info, "remark"));
                editorialMapper.updateById(update);
                break;
            }
            case PHOTO: {
                if (photographyMapper.selectById(workId) == null) throw ApiException.notFound("摄影作品不存在");
                BizPhotographyWork update = new BizPhotographyWork();
                update.setId(workId);
                update.setTitle(getStringVal(info, "title"));
                update.setPlatform(getStringVal(info, "platform"));
                update.setCategory(getStringVal(info, "category"));
                update.setPhotoCount(getIntVal(info, "photoCount"));
                update.setReadCount(getIntVal(info, "readCount"));
                update.setPublishDate(getLocalDateVal(info, "publishDate"));
                update.setArticleLink(getStringVal(info, "articleLink"));
                update.setIsPublished(getIntVal(info, "isPublished"));
                update.setIsSpecialNode(getIntVal(info, "isSpecialNode"));
                update.setSpecialMultiplier(normalizeSpecialMultiplier(info));
                update.setRemark(getStringVal(info, "remark"));
                photographyMapper.updateById(update);
                break;
            }
            case VIDEO: {
                if (videoMapper.selectById(workId) == null) throw ApiException.notFound("视频作品不存在");
                BizVideoWork update = new BizVideoWork();
                update.setId(workId);
                update.setVideoTitle(getStringVal(info, "videoTitle"));
                update.setCategory(getStringVal(info, "category"));
                update.setVideoDurationSec(getIntVal(info, "videoDurationSec"));
                update.setLikeCount(getIntVal(info, "likeCount"));
                update.setReprintStatus(Objects.equals(getIntVal(info, "isReposted"), 1) ? "官方媒体转载" : "无");
                update.setIsComplexEdit(getIntVal(info, "isComplexEdit"));
                update.setPublishDate(getLocalDateVal(info, "publishDate"));
                update.setVideoLink(getStringVal(info, "videoLink"));
                update.setIsPublished(getIntVal(info, "isPublished"));
                update.setIsSpecialNode(getIntVal(info, "isSpecialNode"));
                update.setSpecialMultiplier(normalizeSpecialMultiplier(info));
                update.setRemark(getStringVal(info, "remark"));
                videoMapper.updateById(update);
                break;
            }
            case VISUAL: {
                if (visualMapper.selectById(workId) == null) throw ApiException.notFound("视觉作品不存在");
                BizVisualWork update = new BizVisualWork();
                update.setId(workId);
                update.setTitle(getStringVal(info, "title"));
                update.setCategory(canonicalVisualCategory(getStringVal(info, "category")));
                // 人数和人均金额由参与人员及稿费标准计算，不能接受客户端直接修改。
                update.setPublishDate(getLocalDateVal(info, "publishDate"));
                update.setArticleLink(getStringVal(info, "articleLink"));
                update.setIsPublished(getIntVal(info, "isPublished"));
                update.setIsSpecialNode(getIntVal(info, "isSpecialNode"));
                update.setSpecialMultiplier(normalizeSpecialMultiplier(info));
                update.setRemark(getStringVal(info, "remark"));
                visualMapper.updateById(update);
                break;
            }
            case SUBSIDY: {
                if (monthlySubsidyMapper.selectById(workId) == null) throw ApiException.notFound("月度补贴任务不存在");
                BizMonthlySubsidy update = new BizMonthlySubsidy();
                update.setId(workId);
                update.setTitle(getStringVal(info, "title"));
                update.setCategory(getStringVal(info, "category"));
                update.setSubsidyMonth(getStringVal(info, "subsidyMonth"));
                update.setRemark(getStringVal(info, "remark"));
                monthlySubsidyMapper.updateById(update);
                break;
            }
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }
        clearStoredPublishDateIfUnpublished(workType, workId, info);
    }

    // 202608121145 - 修复了后端仍保存未发布任务发布日期的问题。
    private void normalizeUnpublishedPublishDate(String workType, Map<String, Object> info) {
        if (!SUBSIDY.equals(workType) && !Objects.equals(getIntVal(info, "isPublished"), 1)) {
            info.put("publishDate", null);
        }
    }

    private void clearStoredPublishDateIfUnpublished(String workType, Long workId, Map<String, Object> info) {
        if (SUBSIDY.equals(workType) || Objects.equals(getIntVal(info, "isPublished"), 1)) return;
        switch (workType) {
            case EDITORIAL: clearStoredPublishDate(editorialMapper, workId); break;
            case PHOTO: clearStoredPublishDate(photographyMapper, workId); break;
            case VIDEO: clearStoredPublishDate(videoMapper, workId); break;
            case VISUAL: clearStoredPublishDate(visualMapper, workId); break;
            default: break;
        }
    }

    private <T> void clearStoredPublishDate(BaseMapper<T> mapper, Long workId) {
        UpdateWrapper<T> update = new UpdateWrapper<>();
        update.eq("id", workId).set("publish_date", null);
        mapper.update(null, update);
    }

    private void ensureCanSubmitWorkType(Long userId, Integer roleCode, String workType) {
        String deptCode = workTypeToDeptCode(workType);
        Set<String> allowedCodes = isManagementRole(roleCode)
                ? allFeeDeptCodes()
                : getUserDeptCodes(userId);
        if (!allowedCodes.contains(deptCode)) {
            throw ApiException.forbidden("不能向其他部门提交稿费任务");
        }
    }

    private void ensureCanViewWorkType(Long userId, Integer roleCode, String workType) {
        String deptCode = workTypeToDeptCode(workType);
        if (!allowedFeeDeptCodes(userId, roleCode).contains(deptCode)) {
            throw ApiException.forbidden("不能访问其他部门的稿费任务");
        }
    }

    private Set<String> allowedFeeDeptCodes(Long userId, Integer roleCode) {
        if (isGlobalTaskViewer(roleCode)) {
            return allFeeDeptCodes();
        }
        return getUserDeptCodes(userId);
    }

    private Set<String> allFeeDeptCodes() {
        return new HashSet<>(Arrays.asList("EDITORIAL", "PHOTOGRAPHY", "VIDEO", "VISUAL", "MANAGEMENT"));
    }

    private boolean isGlobalTaskViewer(Integer roleCode) {
        // 部长可以查看和编辑所有部门的任务元数据，但金额仍在服务端统一清空。
        return roleCode != null && (roleCode == 0 || roleCode == 1 || roleCode == 2
                || roleCode == 3 || roleCode == 4);
    }

    private boolean isManagementRole(Integer roleCode) {
        return roleCode != null && (roleCode == 0 || roleCode == 1 || roleCode == 2 || roleCode == 3);
    }

    private boolean canManageFeeAmount(Integer roleCode) {
        return roleCode != null && (roleCode == 0 || roleCode == 3);
    }

    private boolean canEditFeeTask(Integer roleCode) {
        return roleCode != null && (roleCode == 0 || roleCode == 3 || roleCode == 4);
    }

    private String normalizeAuditStatus(String status) {
        return StringUtils.hasText(status) ? status : "SUBMITTED";
    }

    private String summarizeFeeChanges(FeeDetailVO before, FeeDetailVO after) {
        List<String> changed = new ArrayList<>();
        Map<String, Object> beforeInfo = before.getWorkInfo() != null ? before.getWorkInfo() : Collections.emptyMap();
        Map<String, Object> afterInfo = after.getWorkInfo() != null ? after.getWorkInfo() : Collections.emptyMap();
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(beforeInfo.keySet());
        keys.addAll(afterInfo.keySet());
        for (String key : keys) {
            if (!auditValuesEqual(beforeInfo.get(key), afterInfo.get(key))) {
                changed.add(FEE_AUDIT_FIELD_LABELS.getOrDefault(key, key));
            }
        }
        if (!auditValuesEqual(before.getContributors(), after.getContributors())) changed.add("参与人员及金额");
        if (!auditValuesEqual(before.getTotalAmount(), after.getTotalAmount())) changed.add("总金额");
        if (!auditValuesEqual(before.getStatus(), after.getStatus())) changed.add("审核状态");
        if (!auditValuesEqual(before.getRemark(), after.getRemark())) changed.add("备注");
        return changed.isEmpty() ? "无实际变化" : String.join("、", changed);
    }

    private boolean auditValuesEqual(Object left, Object right) {
        if (left instanceof BigDecimal && right instanceof BigDecimal) {
            return ((BigDecimal) left).compareTo((BigDecimal) right) == 0;
        }
        return Objects.equals(left, right);
    }

    private String feeAuditSnapshot(FeeDetailVO detail) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("workType", detail.getWorkType());
        snapshot.put("workInfo", detail.getWorkInfo());
        snapshot.put("contributors", detail.getContributors());
        snapshot.put("status", detail.getStatus());
        snapshot.put("totalAmount", detail.getTotalAmount());
        snapshot.put("remark", detail.getRemark());
        try {
            return AUDIT_OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("稿费变更快照序列化失败", exception);
        }
    }

    private static Map<String, String> createFeeAuditFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("title", "标题");
        labels.put("videoTitle", "视频标题");
        labels.put("platform", "发布平台");
        labels.put("category", "类别");
        labels.put("wordCount", "字数");
        labels.put("readCount", "阅读量");
        labels.put("photoCount", "图片数量");
        labels.put("videoDurationSec", "视频时长");
        labels.put("likeCount", "点赞数");
        labels.put("reprintStatus", "转载情况");
        labels.put("isReposted", "是否官方转载");
        labels.put("isComplexEdit", "是否复杂剪辑");
        labels.put("authorCount", "参与人数");
        labels.put("authorAvgFee", "人均金额");
        labels.put("publishDate", "发布时间");
        labels.put("articleLink", "文章链接");
        labels.put("videoLink", "视频链接");
        labels.put("isPublished", "是否已发布");
        labels.put("isSpecialNode", "特殊节点");
        labels.put("specialMultiplier", "特殊节点倍数");
        labels.put("subsidyMonth", "补贴月份");
        labels.put("remark", "备注");
        return Collections.unmodifiableMap(labels);
    }

    private String getAuditStatus(String workType, Long workId) {
        switch (workType) {
            case EDITORIAL:
                BizEditorialArticle editorial = editorialMapper.selectById(workId);
                if (editorial == null || !Objects.equals(editorial.getIsValid(), 1)) throw ApiException.notFound("稿件不存在或已失效");
                return normalizeAuditStatus(editorial.getAuditStatus());
            case PHOTO:
                BizPhotographyWork photography = photographyMapper.selectById(workId);
                if (photography == null || !Objects.equals(photography.getIsValid(), 1)) throw ApiException.notFound("摄影作品不存在或已失效");
                return normalizeAuditStatus(photography.getAuditStatus());
            case VIDEO:
                BizVideoWork video = videoMapper.selectById(workId);
                if (video == null || !Objects.equals(video.getIsValid(), 1)) throw ApiException.notFound("视频作品不存在或已失效");
                return normalizeAuditStatus(video.getAuditStatus());
            case VISUAL:
                BizVisualWork visual = visualMapper.selectById(workId);
                if (visual == null || !Objects.equals(visual.getIsValid(), 1)) throw ApiException.notFound("视觉作品不存在或已失效");
                return normalizeAuditStatus(visual.getAuditStatus());
            case SUBSIDY:
                BizMonthlySubsidy subsidy = monthlySubsidyMapper.selectById(workId);
                if (subsidy == null || !Objects.equals(subsidy.getIsValid(), 1)) throw ApiException.notFound("月度补贴任务不存在或已失效");
                return normalizeAuditStatus(subsidy.getAuditStatus());
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }
    }

    private void updateAuditStatus(String workType, Long workId, String status) {
        switch (workType) {
            case EDITORIAL:
                BizEditorialArticle editorial = new BizEditorialArticle();
                editorial.setId(workId);
                editorial.setAuditStatus(status);
                editorialMapper.updateById(editorial);
                break;
            case PHOTO:
                BizPhotographyWork photography = new BizPhotographyWork();
                photography.setId(workId);
                photography.setAuditStatus(status);
                photographyMapper.updateById(photography);
                break;
            case VIDEO:
                BizVideoWork video = new BizVideoWork();
                video.setId(workId);
                video.setAuditStatus(status);
                videoMapper.updateById(video);
                break;
            case VISUAL:
                BizVisualWork visual = new BizVisualWork();
                visual.setId(workId);
                visual.setAuditStatus(status);
                visualMapper.updateById(visual);
                break;
            case SUBSIDY:
                BizMonthlySubsidy subsidy = new BizMonthlySubsidy();
                subsidy.setId(workId);
                subsidy.setAuditStatus(status);
                monthlySubsidyMapper.updateById(subsidy);
                break;
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }
    }

    private void updateWorkValidity(String workType, Long workId, Integer isValid) {
        switch (workType) {
            case EDITORIAL:
                BizEditorialArticle editorial = new BizEditorialArticle();
                editorial.setId(workId);
                editorial.setIsValid(isValid);
                editorialMapper.updateById(editorial);
                break;
            case PHOTO:
                BizPhotographyWork photography = new BizPhotographyWork();
                photography.setId(workId);
                photography.setIsValid(isValid);
                photographyMapper.updateById(photography);
                break;
            case VIDEO:
                BizVideoWork video = new BizVideoWork();
                video.setId(workId);
                video.setIsValid(isValid);
                videoMapper.updateById(video);
                break;
            case VISUAL:
                BizVisualWork visual = new BizVisualWork();
                visual.setId(workId);
                visual.setIsValid(isValid);
                visualMapper.updateById(visual);
                break;
            case SUBSIDY:
                BizMonthlySubsidy subsidy = new BizMonthlySubsidy();
                subsidy.setId(workId);
                subsidy.setIsValid(isValid);
                monthlySubsidyMapper.updateById(subsidy);
                break;
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }
    }

    private Map<String, Object> preserveRestrictedFeeFields(String workType, Long workId,
                                                             Map<String, Object> submittedInfo) {
        Map<String, Object> info = new LinkedHashMap<>(submittedInfo);
        switch (workType) {
            case EDITORIAL:
                BizEditorialArticle editorial = editorialMapper.selectById(workId);
                info.put("isSpecialNode", editorial.getIsSpecialNode());
                info.put("specialMultiplier", editorial.getSpecialMultiplier());
                break;
            case PHOTO:
                BizPhotographyWork photography = photographyMapper.selectById(workId);
                info.put("isSpecialNode", photography.getIsSpecialNode());
                info.put("specialMultiplier", photography.getSpecialMultiplier());
                break;
            case VIDEO:
                BizVideoWork video = videoMapper.selectById(workId);
                info.put("isSpecialNode", video.getIsSpecialNode());
                info.put("specialMultiplier", video.getSpecialMultiplier());
                break;
            case VISUAL:
                BizVisualWork visual = visualMapper.selectById(workId);
                info.put("isSpecialNode", visual.getIsSpecialNode());
                info.put("specialMultiplier", visual.getSpecialMultiplier());
                info.put("authorAvgFee", visual.getAuthorAvgFee());
                break;
            case SUBSIDY:
                break;
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }
        return info;
    }

    private Set<String> getUserDeptCodes(Long userId) {
        QueryWrapper<SysUserDept> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        List<SysUserDept> userDepts = sysUserDeptMapper.selectList(query);
        if (userDepts == null || userDepts.isEmpty()) {
            return Collections.emptySet();
        }
        return userDepts.stream()
                .map(SysUserDept::getDeptId)
                .map(sysDeptMapper::selectById)
                .filter(Objects::nonNull)
                .map(SysDept::getDeptCode)
                .collect(Collectors.toSet());
    }

    private String workTypeToDeptCode(String workType) {
        switch (workType) {
            case EDITORIAL:
                return "EDITORIAL";
            case PHOTO:
                return "PHOTOGRAPHY";
            case VIDEO:
                return "VIDEO";
            case VISUAL:
                return "VISUAL";
            case SUBSIDY:
                return "MANAGEMENT";
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }
    }

    private String feeDeptFilterBase(String deptFilter) {
        if (!StringUtils.hasText(deptFilter)) return null;
        int separator = deptFilter.indexOf(':');
        return separator >= 0 ? deptFilter.substring(0, separator) : deptFilter;
    }

    private String feePlatformFilter(String deptFilter) {
        if (!StringUtils.hasText(deptFilter)) return null;
        int separator = deptFilter.indexOf(':');
        return separator >= 0 && separator < deptFilter.length() - 1
                ? deptFilter.substring(separator + 1)
                : null;
    }

    private String workTypeDisplayName(String workType) {
        switch (workType) {
            case EDITORIAL:
                return "采编部";
            case PHOTO:
                return "摄影部";
            case VIDEO:
                return "视频部";
            case VISUAL:
                return "视觉部";
            case SUBSIDY:
                return "月度补贴";
            default:
                return workType;
        }
    }

    private List<FeeListVO> queryEditorialList(String month) {
        QueryWrapper<BizEditorialArticle> query = new QueryWrapper<>();
        query.eq("is_valid", 1);
        if (StringUtils.hasText(month)) {
            try {
                YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
                LocalDate start = ym.atDay(1);
                LocalDate end = ym.atEndOfMonth();
                query.ge("publish_date", start).le("publish_date", end);
            } catch (Exception e) {
                log.warn("Invalid month format: {}", month);
            }
        }
        query.orderByDesc("create_time");
        List<BizEditorialArticle> articles = editorialMapper.selectList(query);

        return articles.stream().map(a -> {
            FeeListVO vo = new FeeListVO();
            vo.setId(a.getId());
            vo.setWorkType(EDITORIAL);
            vo.setTitle(a.getTitle());
            vo.setDeptName(workTypeDisplayName(EDITORIAL));
            vo.setPlatform(a.getPlatform());
            vo.setPublishDate(a.getPublishDate());
            vo.setTotalAmount(a.getTotalAmount());
            vo.setStatus(normalizeAuditStatus(a.getAuditStatus()));
            vo.setCreateTime(a.getCreateTime());
            // Resolve submitter from first contributor
            resolveSubmitter(vo, a.getSubmitterId());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<FeeListVO> queryPhotographyList(String month) {
        QueryWrapper<BizPhotographyWork> query = new QueryWrapper<>();
        query.eq("is_valid", 1);
        if (StringUtils.hasText(month)) {
            try {
                YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
                query.ge("publish_date", ym.atDay(1)).le("publish_date", ym.atEndOfMonth());
            } catch (Exception e) {
                log.warn("Invalid month format: {}", month);
            }
        }
        query.orderByDesc("create_time");
        List<BizPhotographyWork> works = photographyMapper.selectList(query);

        return works.stream().map(w -> {
            FeeListVO vo = new FeeListVO();
            vo.setId(w.getId());
            vo.setWorkType(PHOTO);
            vo.setTitle(w.getTitle());
            vo.setDeptName(workTypeDisplayName(PHOTO));
            vo.setPlatform(w.getPlatform());
            vo.setPublishDate(w.getPublishDate());
            vo.setTotalAmount(w.getTotalAmount());
            vo.setStatus(normalizeAuditStatus(w.getAuditStatus()));
            vo.setCreateTime(w.getCreateTime());
            resolveSubmitter(vo, w.getSubmitterId());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<FeeListVO> queryVideoList(String month) {
        QueryWrapper<BizVideoWork> query = new QueryWrapper<>();
        query.eq("is_valid", 1);
        if (StringUtils.hasText(month)) {
            try {
                YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
                query.ge("publish_date", ym.atDay(1)).le("publish_date", ym.atEndOfMonth());
            } catch (Exception e) {
                log.warn("Invalid month format: {}", month);
            }
        }
        query.orderByDesc("create_time");
        List<BizVideoWork> works = videoMapper.selectList(query);

        return works.stream().map(w -> {
            FeeListVO vo = new FeeListVO();
            vo.setId(w.getId());
            vo.setWorkType(VIDEO);
            vo.setTitle(w.getVideoTitle());
            vo.setDeptName(workTypeDisplayName(VIDEO));
            vo.setPublishDate(w.getPublishDate());
            vo.setTotalAmount(w.getTotalAmount());
            vo.setStatus(normalizeAuditStatus(w.getAuditStatus()));
            vo.setCreateTime(w.getCreateTime());
            resolveSubmitter(vo, w.getSubmitterId());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<FeeListVO> queryVisualList(String month) {
        QueryWrapper<BizVisualWork> query = new QueryWrapper<>();
        query.eq("is_valid", 1);
        if (StringUtils.hasText(month)) {
            try {
                YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
                query.ge("publish_date", ym.atDay(1)).le("publish_date", ym.atEndOfMonth());
            } catch (Exception e) {
                log.warn("Invalid month format: {}", month);
            }
        }
        query.orderByDesc("create_time");
        List<BizVisualWork> works = visualMapper.selectList(query);

        return works.stream().map(w -> {
            FeeListVO vo = new FeeListVO();
            vo.setId(w.getId());
            vo.setWorkType(VISUAL);
            vo.setTitle(w.getTitle());
            vo.setDeptName(workTypeDisplayName(VISUAL));
            vo.setPublishDate(w.getPublishDate());
            vo.setTotalAmount(w.getTotalAmount());
            vo.setStatus(normalizeAuditStatus(w.getAuditStatus()));
            vo.setCreateTime(w.getCreateTime());
            resolveSubmitter(vo, w.getSubmitterId());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<FeeListVO> queryMonthlySubsidyList(String month) {
        QueryWrapper<BizMonthlySubsidy> query = new QueryWrapper<>();
        query.eq("is_valid", 1);
        if (StringUtils.hasText(month)) {
            query.eq("subsidy_month", month);
        }
        query.orderByDesc("create_time");
        return monthlySubsidyMapper.selectList(query).stream().map(item -> {
            FeeListVO vo = new FeeListVO();
            vo.setId(item.getId());
            vo.setWorkType(SUBSIDY);
            vo.setTitle(item.getTitle());
            vo.setDeptName(workTypeDisplayName(SUBSIDY));
            vo.setTotalAmount(item.getTotalAmount());
            vo.setStatus(normalizeAuditStatus(item.getAuditStatus()));
            vo.setCreateTime(item.getCreateTime());
            resolveSubmitter(vo, item.getSubmitterId());
            return vo;
        }).collect(Collectors.toList());
    }

    private void resolveSubmitter(FeeListVO vo, Long submitterId) {
        SysUser user = submitterId != null ? sysUserMapper.selectById(submitterId) : null;
        vo.setSubmitterName(user != null ? user.getRealName() : "未知");
    }

    private Set<String> findTasksByContributorName(String contributorName) {
        QueryWrapper<SysUser> userQuery = new QueryWrapper<>();
        userQuery.select("id")
                .like("real_name", contributorName.trim());
        List<Long> userIds = sysUserMapper.selectList(userQuery).stream()
                .map(SysUser::getId)
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return Collections.emptySet();

        QueryWrapper<BizWorkContributor> contributorQuery = new QueryWrapper<>();
        contributorQuery.select("work_type", "work_id").in("user_id", userIds);
        return contributorMapper.selectList(contributorQuery).stream()
                .map(item -> feeTaskKey(item.getWorkType(), item.getWorkId()))
                .collect(Collectors.toSet());
    }

    private void ensureNotDuplicate(String workType, Map<String, Object> info,
                                    List<FeeSubmitDTO.ContributorDTO> contributors, Long excludedId) {
        boolean duplicate;
        switch (workType) {
            case EDITORIAL: {
                QueryWrapper<BizEditorialArticle> query = new QueryWrapper<>();
                query.eq("is_valid", 1);
                applyLinkOrIdentity(query, "article_link", getStringVal(info, "articleLink"),
                        "title", getStringVal(info, "title"),
                        "platform", getStringVal(info, "platform"),
                        "publish_date", getLocalDateVal(info, "publishDate"));
                if (excludedId != null) query.ne("id", excludedId);
                duplicate = editorialMapper.selectCount(query) > 0;
                break;
            }
            case PHOTO: {
                QueryWrapper<BizPhotographyWork> query = new QueryWrapper<>();
                query.eq("is_valid", 1);
                applyLinkOrIdentity(query, "article_link", getStringVal(info, "articleLink"),
                        "title", getStringVal(info, "title"),
                        "platform", getStringVal(info, "platform"),
                        "publish_date", getLocalDateVal(info, "publishDate"));
                if (excludedId != null) query.ne("id", excludedId);
                duplicate = photographyMapper.selectCount(query) > 0;
                break;
            }
            case VIDEO: {
                QueryWrapper<BizVideoWork> query = new QueryWrapper<>();
                query.eq("is_valid", 1);
                applyLinkOrIdentity(query, "video_link", getStringVal(info, "videoLink"),
                        "video_title", getStringVal(info, "videoTitle"),
                        "category", getStringVal(info, "category"),
                        "publish_date", getLocalDateVal(info, "publishDate"));
                if (excludedId != null) query.ne("id", excludedId);
                duplicate = videoMapper.selectCount(query) > 0;
                break;
            }
            case VISUAL: {
                QueryWrapper<BizVisualWork> query = new QueryWrapper<>();
                query.eq("is_valid", 1);
                applyLinkOrIdentity(query, "article_link", getStringVal(info, "articleLink"),
                        "title", getStringVal(info, "title"),
                        "category", canonicalVisualCategory(getStringVal(info, "category")),
                        "publish_date", getLocalDateVal(info, "publishDate"));
                if (excludedId != null) query.ne("id", excludedId);
                duplicate = visualMapper.selectCount(query) > 0;
                break;
            }
            case SUBSIDY:
                duplicate = hasDuplicateSubsidy(info, contributors, excludedId);
                break;
            default:
                throw ApiException.badRequest("不支持的作品类型: " + workType);
        }
        if (duplicate) {
            throw ApiException.conflict("检测到相同作品或同月重复补贴，请勿重复提交");
        }
    }

    private <T> void applyLinkOrIdentity(QueryWrapper<T> query,
                                         String linkColumn, String link,
                                         String titleColumn, String title,
                                         String categoryColumn, String category,
                                         String dateColumn, LocalDate publishDate) {
        if (StringUtils.hasText(link)) {
            query.eq(linkColumn, link.trim());
            return;
        }
        query.eq(titleColumn, title)
                .eq(categoryColumn, category);
        if (publishDate == null) {
            query.isNull(dateColumn);
        } else {
            query.eq(dateColumn, publishDate);
        }
    }

    private boolean hasDuplicateSubsidy(Map<String, Object> info,
                                        List<FeeSubmitDTO.ContributorDTO> contributors,
                                        Long excludedId) {
        if (contributors == null || contributors.isEmpty()) return false;
        QueryWrapper<BizMonthlySubsidy> taskQuery = new QueryWrapper<>();
        taskQuery.select("id")
                .eq("is_valid", 1)
                .eq("category", getStringVal(info, "category"))
                .eq("subsidy_month", getStringVal(info, "subsidyMonth"));
        if (excludedId != null) taskQuery.ne("id", excludedId);
        List<Long> workIds = monthlySubsidyMapper.selectList(taskQuery).stream()
                .map(BizMonthlySubsidy::getId)
                .collect(Collectors.toList());
        if (workIds.isEmpty()) return false;
        Set<Long> userIds = contributors.stream()
                .map(FeeSubmitDTO.ContributorDTO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return false;
        QueryWrapper<BizWorkContributor> contributorQuery = new QueryWrapper<>();
        contributorQuery.eq("work_type", SUBSIDY)
                .in("work_id", workIds)
                .in("user_id", userIds);
        return contributorMapper.selectCount(contributorQuery) > 0;
    }

    private String canonicalVisualCategory(String category) {
        if ("互动、长图、SVG、手绘".equals(category) || "互动长图SVG手绘".equals(category)) {
            return "互动、长图、SVG动图、手绘";
        }
        return category;
    }

    // ======================== Map value extraction helpers ========================

    private String getStringVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getIntVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer normalizeSpecialMultiplier(Map<String, Object> info) {
        if (!Objects.equals(getIntVal(info, "isSpecialNode"), 1)) return 1;
        Integer multiplier = getIntVal(info, "specialMultiplier");
        if (multiplier == null || (multiplier != 2 && multiplier != 3)) {
            throw ApiException.badRequest("特殊节点倍数只能选择 2 倍或 3 倍");
        }
        return multiplier;
    }

    private BigDecimal getBigDecimalVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? "" : normalizeMoney(value).toPlainString();
    }

    private LocalDate getLocalDateVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        try {
            return LocalDate.parse(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Simple URL encoder for file name.
     */
    private String URLEncoder_encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
