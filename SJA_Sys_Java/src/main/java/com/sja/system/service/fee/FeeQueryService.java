package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.common.PageResult;
import com.sja.system.dto.FeeTaskRefDTO;
import com.sja.system.entity.BizEditorialArticle;
import com.sja.system.entity.BizMonthlySubsidy;
import com.sja.system.entity.BizPhotographyWork;
import com.sja.system.entity.BizVideoWork;
import com.sja.system.entity.BizVisualWork;
import com.sja.system.entity.BizWorkContributor;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.BizEditorialArticleMapper;
import com.sja.system.mapper.BizMonthlySubsidyMapper;
import com.sja.system.mapper.BizPhotographyWorkMapper;
import com.sja.system.mapper.BizVideoWorkMapper;
import com.sja.system.mapper.BizVisualWorkMapper;
import com.sja.system.mapper.BizWorkContributorMapper;
import com.sja.system.mapper.FeeQueryMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.model.FeeListQuery;
import com.sja.system.mapper.model.FeeListRow;
import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 稿费列表、详情以及导出数据批次的只读查询服务。
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
// 202609201644 - 将稿费查询、日期筛选和展示转换从综合服务中独立出来。
public class FeeQueryService {

    private final BizEditorialArticleMapper editorialMapper;
    private final BizPhotographyWorkMapper photographyMapper;
    private final BizVideoWorkMapper videoMapper;
    private final BizVisualWorkMapper visualMapper;
    private final BizMonthlySubsidyMapper monthlySubsidyMapper;
    private final BizWorkContributorMapper contributorMapper;
    private final FeeQueryMapper feeQueryMapper;
    private final SysUserMapper sysUserMapper;
    private final FeeAccessService accessService;

    public PageResult<FeeListVO> list(String deptCode, String month, String status, String contributorName,
                                      String createDate, String publishStartDate, String publishEndDate,
                                      String createStartDate, String createEndDate, Integer pageNum,
                                      Integer pageSize, Long operatorId, Integer roleCode) {
        DateFilters dates = parseDateFilters(createDate, publishStartDate, publishEndDate,
                createStartDate, createEndDate);
        int currentPage = pageNum != null ? Math.max(1, pageNum) : 1;
        int size = pageSize != null ? Math.min(200, Math.max(1, pageSize)) : 10;
        FeeListQuery query = createQuery(deptCode, month, status, contributorName, dates,
                operatorId, roleCode, Collections.emptyList());
        if (query == null) {
            return new PageResult<>(Collections.emptyList(), 0L, (long) currentPage, (long) size);
        }
        long total = feeQueryMapper.countFeeItems(query);
        List<FeeListVO> records = total == 0 ? Collections.emptyList()
                : loadBatch(query, (long) (currentPage - 1) * size, size);
        if (!accessService.canManageAmount(roleCode)) {
            records.forEach(item -> item.setTotalAmount(null));
        }
        return new PageResult<>(records, total, (long) currentPage, (long) size);
    }

    public FeeDetailVO detail(Long id, String workTypeCode, Long operatorId, Integer roleCode) {
        accessService.ensureCanView(operatorId, roleCode, workTypeCode);
        boolean canSeeAmount = accessService.canManageAmount(roleCode);
        FeeDetailVO result = new FeeDetailVO();
        result.setId(id);
        result.setWorkType(workTypeCode);
        Map<String, Object> workInfo = new LinkedHashMap<>();
        BigDecimal totalAmount;

        switch (FeeWorkType.require(workTypeCode)) {
            case EDITORIAL -> {
                BizEditorialArticle entity = editorialMapper.selectById(id);
                requireValid(entity, entity != null ? entity.getIsValid() : null, "稿件不存在或已失效");
                workInfo.put("title", entity.getTitle());
                workInfo.put("platform", entity.getPlatform());
                workInfo.put("category", entity.getCategory());
                if ("微信".equals(entity.getPlatform())) workInfo.put("readCount", entity.getReadCount());
                if ("校报".equals(entity.getPlatform())) workInfo.put("wordCount", entity.getWordCount());
                putPublishingFields(workInfo, entity.getPublishDate(), entity.getArticleLink(),
                        entity.getIsPublished(), entity.getIsSpecialNode(), entity.getSpecialMultiplier(), canSeeAmount);
                totalAmount = amount(entity.getTotalAmount());
                result.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                result.setRemark(entity.getRemark());
            }
            case PHOTO -> {
                BizPhotographyWork entity = photographyMapper.selectById(id);
                requireValid(entity, entity != null ? entity.getIsValid() : null, "摄影作品不存在或已失效");
                workInfo.put("title", entity.getTitle());
                workInfo.put("platform", entity.getPlatform());
                if ("微信".equals(entity.getPlatform())) {
                    workInfo.put("readCount", entity.getReadCount());
                    workInfo.put("category", entity.getCategory());
                }
                putPublishingFields(workInfo, entity.getPublishDate(), entity.getArticleLink(),
                        entity.getIsPublished(), entity.getIsSpecialNode(), entity.getSpecialMultiplier(), canSeeAmount);
                totalAmount = amount(entity.getTotalAmount());
                result.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                result.setRemark(entity.getRemark());
            }
            case VIDEO -> {
                BizVideoWork entity = videoMapper.selectById(id);
                requireValid(entity, entity != null ? entity.getIsValid() : null, "视频作品不存在或已失效");
                workInfo.put("videoTitle", entity.getVideoTitle());
                workInfo.put("category", entity.getCategory());
                workInfo.put("videoDurationSec", entity.getVideoDurationSec());
                workInfo.put("likeCount", entity.getLikeCount());
                workInfo.put("isReposted", "官方媒体转载".equals(entity.getReprintStatus()) ? 1 : 0);
                workInfo.put("isComplexEdit", entity.getIsComplexEdit());
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("videoLink", entity.getVideoLink());
                workInfo.put("isPublished", entity.getIsPublished());
                putSensitiveFields(workInfo, entity.getIsSpecialNode(), entity.getSpecialMultiplier(), canSeeAmount);
                totalAmount = amount(entity.getTotalAmount());
                result.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                result.setRemark(entity.getRemark());
            }
            case VISUAL -> {
                BizVisualWork entity = visualMapper.selectById(id);
                requireValid(entity, entity != null ? entity.getIsValid() : null, "视觉作品不存在或已失效");
                workInfo.put("title", entity.getTitle());
                workInfo.put("category", entity.getCategory());
                workInfo.put("authorCount", entity.getAuthorCount());
                if (canSeeAmount) workInfo.put("authorAvgFee", entity.getAuthorAvgFee());
                putPublishingFields(workInfo, entity.getPublishDate(), entity.getArticleLink(),
                        entity.getIsPublished(), entity.getIsSpecialNode(), entity.getSpecialMultiplier(), canSeeAmount);
                totalAmount = amount(entity.getTotalAmount());
                result.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                result.setRemark(entity.getRemark());
            }
            case SUBSIDY -> {
                BizMonthlySubsidy entity = monthlySubsidyMapper.selectById(id);
                requireValid(entity, entity != null ? entity.getIsValid() : null, "月度补贴任务不存在或已失效");
                workInfo.put("title", entity.getTitle());
                workInfo.put("category", entity.getCategory());
                workInfo.put("subsidyMonth", entity.getSubsidyMonth());
                totalAmount = amount(entity.getTotalAmount());
                result.setStatus(normalizeAuditStatus(entity.getAuditStatus()));
                result.setRemark(entity.getRemark());
            }
            default -> throw ApiException.badRequest("不支持的作品类型: " + workTypeCode);
        }

        result.setWorkInfo(workInfo);
        result.setTotalAmount(canSeeAmount ? totalAmount : null);
        result.setContributors(loadContributors(workTypeCode, id, canSeeAmount));
        return result;
    }

    public FeeListQuery createExportQuery(String deptCode, String month, String status,
                                           String contributorName, String createDate,
                                           String publishStartDate, String publishEndDate,
                                           String createStartDate, String createEndDate,
                                           Long operatorId, Integer roleCode,
                                           List<FeeTaskRefDTO> selectedTasks) {
        DateFilters dates = parseDateFilters(createDate, publishStartDate, publishEndDate,
                createStartDate, createEndDate);
        return createQuery(deptCode, month, status, contributorName, dates,
                operatorId, roleCode, selectedTasks);
    }

    public boolean applyTemplateConstraint(FeeListQuery query, String templateType) {
        FeeWorkType requiredType;
        String requiredPlatform = null;
        switch (templateType) {
            case "EDITORIAL_WECHAT" -> { requiredType = FeeWorkType.EDITORIAL; requiredPlatform = "微信"; }
            case "EDITORIAL_WEBSITE" -> { requiredType = FeeWorkType.EDITORIAL; requiredPlatform = "院网"; }
            case "EDITORIAL_NEWSPAPER" -> { requiredType = FeeWorkType.EDITORIAL; requiredPlatform = "校报"; }
            case "PHOTO" -> requiredType = FeeWorkType.PHOTO;
            case "VISUAL" -> requiredType = FeeWorkType.VISUAL;
            case "VIDEO" -> requiredType = FeeWorkType.VIDEO;
            default -> { return true; }
        }
        if (!query.getWorkTypes().contains(requiredType.code())
                || (requiredPlatform != null && query.getPlatform() != null
                && !requiredPlatform.equals(query.getPlatform()))) {
            return false;
        }
        query.setWorkTypes(Collections.singleton(requiredType.code()));
        if (requiredPlatform != null) query.setPlatform(requiredPlatform);
        return true;
    }

    public long count(FeeListQuery query) {
        return feeQueryMapper.countFeeItems(query);
    }

    public List<FeeListVO> loadBatch(FeeListQuery query, long offset, int batchSize) {
        return toListItems(feeQueryMapper.selectFeeItems(query, offset, batchSize));
    }

    public FeeListQuery copyForWorkType(FeeListQuery source, String workType) {
        FeeListQuery target = new FeeListQuery();
        target.setWorkTypes(Collections.singleton(workType));
        target.setPlatform(source.getPlatform());
        target.setMonth(source.getMonth());
        target.setMonthStart(source.getMonthStart());
        target.setMonthEnd(source.getMonthEnd());
        target.setPublishStart(source.getPublishStart());
        target.setPublishEnd(source.getPublishEnd());
        target.setPublishStartMonth(source.getPublishStartMonth());
        target.setPublishEndMonth(source.getPublishEndMonth());
        target.setCreateStart(source.getCreateStart());
        target.setCreateEndExclusive(source.getCreateEndExclusive());
        target.setStatus(source.getStatus());
        target.setContributorName(source.getContributorName());
        target.setSelectedTasks(source.getSelectedTasks());
        return target;
    }

    private FeeListQuery createQuery(String deptCode, String month, String status, String contributorName,
                                     DateFilters dates, Long operatorId, Integer roleCode,
                                     List<FeeTaskRefDTO> selectedTasks) {
        String selectedDepartment = baseDepartmentFilter(deptCode);
        Set<String> allowedDepartments = accessService.allowedDepartmentCodes(operatorId, roleCode);
        if (StringUtils.hasText(deptCode)) {
            if (!allowedDepartments.contains(selectedDepartment)) return null;
            allowedDepartments = Collections.singleton(selectedDepartment);
        }
        FeeListQuery query = new FeeListQuery();
        query.setWorkTypes(FeeWorkType.codesForDepartments(allowedDepartments));
        query.setPlatform(platformFilter(deptCode));
        query.setStatus(StringUtils.hasText(status) ? status : null);
        query.setContributorName(StringUtils.hasText(contributorName) ? contributorName.trim() : null);
        query.setCreateStart(dates.createStart() != null ? dates.createStart().atStartOfDay() : null);
        query.setCreateEndExclusive(dates.createEnd() != null ? dates.createEnd().plusDays(1).atStartOfDay() : null);
        query.setPublishStart(dates.publishStart());
        query.setPublishEnd(dates.publishEnd());
        query.setPublishStartMonth(dates.publishStart() != null ? YearMonth.from(dates.publishStart()).toString() : null);
        query.setPublishEndMonth(dates.publishEnd() != null ? YearMonth.from(dates.publishEnd()).toString() : null);
        query.setSelectedTasks(selectedTasks != null ? new ArrayList<>(selectedTasks) : Collections.emptyList());
        applyMonthFilter(query, month);
        return query;
    }

    private List<FeeListVO> toListItems(List<FeeListRow> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        List<FeeListVO> items = rows.stream().map(row -> {
            FeeListVO item = new FeeListVO();
            item.setId(row.getId());
            item.setWorkType(row.getWorkType());
            item.setTitle(row.getTitle());
            item.setDeptName(FeeWorkType.require(row.getWorkType()).departmentName());
            item.setPlatform(row.getPlatform());
            item.setPublishDate(row.getPublishDate());
            item.setTotalAmount(row.getTotalAmount());
            item.setStatus(normalizeAuditStatus(row.getAuditStatus()));
            item.setIsPublished(row.getIsPublished());
            item.setCreateTime(row.getCreateTime());
            return item;
        }).collect(Collectors.toList());
        populateSubmitterNames(items, rows);
        return items;
    }

    private void populateSubmitterNames(List<FeeListVO> items, List<FeeListRow> rows) {
        Set<Long> ids = rows.stream().map(FeeListRow::getSubmitterId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = ids.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        for (int index = 0; index < items.size(); index++) {
            Long submitterId = rows.get(index).getSubmitterId();
            items.get(index).setSubmitterName(submitterId == null
                    ? "未知" : names.getOrDefault(submitterId, "未知"));
        }
    }

    private List<FeeDetailVO.ContributorVO> loadContributors(String workType, Long workId,
                                                              boolean canSeeAmount) {
        QueryWrapper<BizWorkContributor> query = new QueryWrapper<>();
        query.eq("work_type", workType).eq("work_id", workId);
        List<BizWorkContributor> contributors = contributorMapper.selectList(query);
        Set<Long> userIds = contributors.stream().map(BizWorkContributor::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> users = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user));
        return contributors.stream().map(contributor -> {
            FeeDetailVO.ContributorVO result = new FeeDetailVO.ContributorVO();
            result.setUserId(contributor.getUserId());
            result.setRoleInWork(contributor.getRoleInWork());
            result.setFeeAmount(canSeeAmount ? contributor.getFeeAmount() : null);
            result.setWorkCount(contributor.getWorkCount());
            SysUser user = users.get(contributor.getUserId());
            result.setRealName(user != null ? user.getRealName() : "未知");
            result.setStudentNo(user != null ? user.getStudentNo() : "");
            return result;
        }).collect(Collectors.toList());
    }

    private void putPublishingFields(Map<String, Object> target, Object publishDate, String link,
                                     Integer published, Integer specialNode, Integer multiplier,
                                     boolean canSeeAmount) {
        target.put("publishDate", publishDate);
        target.put("articleLink", link);
        target.put("isPublished", published);
        putSensitiveFields(target, specialNode, multiplier, canSeeAmount);
    }

    private void putSensitiveFields(Map<String, Object> target, Integer specialNode,
                                    Integer multiplier, boolean canSeeAmount) {
        if (canSeeAmount) {
            target.put("isSpecialNode", specialNode);
            target.put("specialMultiplier", multiplier);
        }
    }

    private void requireValid(Object entity, Integer valid, String message) {
        if (entity == null || !Objects.equals(valid, 1)) throw ApiException.notFound(message);
    }

    private BigDecimal amount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String normalizeAuditStatus(String status) {
        return StringUtils.hasText(status) ? status : "SUBMITTED";
    }

    private void applyMonthFilter(FeeListQuery query, String month) {
        if (!StringUtils.hasText(month)) return;
        try {
            YearMonth parsed = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
            query.setMonth(month);
            query.setMonthStart(parsed.atDay(1));
            query.setMonthEnd(parsed.atEndOfMonth());
        } catch (RuntimeException exception) {
            log.warn("稿费月份格式不正确: {}", month);
        }
    }

    private String baseDepartmentFilter(String filter) {
        if (!StringUtils.hasText(filter)) return null;
        int separator = filter.indexOf(':');
        return separator >= 0 ? filter.substring(0, separator) : filter;
    }

    private String platformFilter(String filter) {
        if (!StringUtils.hasText(filter)) return null;
        int separator = filter.indexOf(':');
        return separator >= 0 && separator < filter.length() - 1
                ? filter.substring(separator + 1) : null;
    }

    private LocalDate parseDate(String value, String label) {
        if (!StringUtils.hasText(value)) return null;
        try {
            if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new java.time.DateTimeException("日期格式错误");
            }
            return LocalDate.parse(value);
        } catch (java.time.DateTimeException exception) {
            throw ApiException.badRequest(label + "格式不正确，请使用 yyyy-MM-dd");
        }
    }

    private DateFilters parseDateFilters(String legacyCreateDate, String publishStart, String publishEnd,
                                         String createStart, String createEnd) {
        LocalDate publishFrom = parseDate(publishStart, "发布开始日期");
        LocalDate publishTo = parseDate(publishEnd, "发布结束日期");
        LocalDate createFrom = parseDate(createStart, "创建开始日期");
        LocalDate createTo = parseDate(createEnd, "创建结束日期");
        if (createFrom == null && createTo == null) {
            createFrom = parseDate(legacyCreateDate, "创建日期");
            createTo = createFrom;
        }
        if (publishFrom != null && publishTo != null && publishFrom.isAfter(publishTo)) {
            throw ApiException.badRequest("发布开始日期不能晚于结束日期");
        }
        if (createFrom != null && createTo != null && createFrom.isAfter(createTo)) {
            throw ApiException.badRequest("创建开始日期不能晚于结束日期");
        }
        return new DateFilters(publishFrom, publishTo, createFrom, createTo);
    }

    private record DateFilters(LocalDate publishStart, LocalDate publishEnd,
                               LocalDate createStart, LocalDate createEnd) {
    }
}
