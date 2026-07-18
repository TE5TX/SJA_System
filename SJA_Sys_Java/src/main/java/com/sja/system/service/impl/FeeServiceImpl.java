package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.excel.EasyExcel;
import com.sja.system.common.PageResult;
import com.sja.system.dto.FeeAuditDTO;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.entity.*;
import com.sja.system.mapper.*;
import com.sja.system.service.FeeService;
import com.sja.system.service.OperationLogService;
import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
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

    private static final String EDITORIAL = "EDITORIAL";
    private static final String PHOTO = "PHOTO";
    private static final String VIDEO = "VIDEO";
    private static final String VISUAL = "VISUAL";

    private final BizEditorialArticleMapper editorialMapper;
    private final BizPhotographyWorkMapper photographyMapper;
    private final BizVideoWorkMapper videoMapper;
    private final BizVisualWorkMapper visualMapper;
    private final BizWorkContributorMapper contributorMapper;
    private final BizFeeStandardMapper feeStandardMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final OperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFee(Long userId, String username, String realName, Integer roleCode, FeeSubmitDTO dto) {
        String workType = dto.getWorkType();
        Map<String, Object> workInfo = dto.getWorkInfo();
        List<FeeSubmitDTO.ContributorDTO> contributors = dto.getContributors();
        ensureCanAccessWorkType(userId, roleCode, workType);
        boolean canSeeAmount = canManageFeeAmount(roleCode);

        Long workId;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 1. Create the work entity based on type
        switch (workType) {
            case EDITORIAL:
                workId = createEditorialWork(workInfo);
                break;
            case PHOTO:
                workId = createPhotographyWork(workInfo);
                break;
            case VIDEO:
                workId = createVideoWork(workInfo);
                break;
            case VISUAL:
                workId = createVisualWork(workInfo);
                break;
            default:
                throw new RuntimeException("不支持的作品类型: " + workType);
        }

        // 2. Create contributor records
        if (contributors != null && !contributors.isEmpty()) {
            for (FeeSubmitDTO.ContributorDTO c : contributors) {
                BizWorkContributor contributor = new BizWorkContributor();
                contributor.setWorkType(workType);
                contributor.setWorkId(workId);
                contributor.setUserId(c.getUserId());
                contributor.setRoleInWork(c.getRoleInWork());
                contributor.setFeeAmount(canSeeAmount ? c.getFeeAmount() : BigDecimal.ZERO);
                contributor.setRemark(c.getRemark());
                contributorMapper.insert(contributor);

                if (canSeeAmount && c.getFeeAmount() != null) {
                    totalAmount = totalAmount.add(c.getFeeAmount());
                }
            }
        }

        // 3. Update total amount on the work entity
        updateWorkTotalAmount(workType, workId, totalAmount);

        // 4. Log operation
        operationLogService.log(userId, realName, "", "CREATE",
                "FEE", workId,
                String.format("提交稿费申请 [%s] 总金额: %.2f, 贡献者: %d人",
                        workType, totalAmount, contributors != null ? contributors.size() : 0),
                "");

        log.info("Fee submitted: type={}, id={}, total={}, contributors={}",
                workType, workId, totalAmount, contributors != null ? contributors.size() : 0);
    }

    @Override
    public PageResult<FeeListVO> getFeeList(String deptCode, String month, String status,
                                            Integer pageNum, Integer pageSize,
                                            Long operatorId, Integer roleCode) {
        int pn = pageNum != null ? pageNum : 1;
        int ps = pageSize != null ? pageSize : 10;

        List<FeeListVO> allItems = new ArrayList<>();

        // Query each work type and combine results
        allItems.addAll(queryEditorialList(month));
        allItems.addAll(queryPhotographyList(month));
        allItems.addAll(queryVideoList(month));
        allItems.addAll(queryVisualList(month));

        Set<String> allowedDeptCodes = allowedFeeDeptCodes(operatorId, roleCode);
        if (StringUtils.hasText(deptCode)) {
            if (!allowedDeptCodes.contains(deptCode)) {
                return new PageResult<>(Collections.emptyList(), 0L, (long) pn, (long) ps);
            }
            allowedDeptCodes = Collections.singleton(deptCode);
        }
        Set<String> finalAllowedDeptCodes = allowedDeptCodes;
        allItems = allItems.stream()
                .filter(item -> finalAllowedDeptCodes.contains(workTypeToDeptCode(item.getWorkType())))
                .collect(Collectors.toList());

        if (!canManageFeeAmount(roleCode)) {
            allItems.forEach(item -> item.setTotalAmount(null));
        }

        // Sort by createTime desc
        allItems.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

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
        ensureCanAccessWorkType(operatorId, roleCode, workType);
        boolean canSeeAmount = canManageFeeAmount(roleCode);
        FeeDetailVO vo = new FeeDetailVO();
        vo.setId(id);
        vo.setWorkType(workType);

        Map<String, Object> workInfo = new LinkedHashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        switch (workType) {
            case EDITORIAL: {
                BizEditorialArticle entity = editorialMapper.selectById(id);
                if (entity == null) throw new RuntimeException("稿件不存在");
                workInfo.put("title", entity.getTitle());
                workInfo.put("platform", entity.getPlatform());
                workInfo.put("category", entity.getCategory());
                workInfo.put("wordCount", entity.getWordCount());
                workInfo.put("readCount", entity.getReadCount());
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("articleLink", entity.getArticleLink());
                workInfo.put("isPublished", entity.getIsPublished());
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setRemark(entity.getRemark());
                break;
            }
            case PHOTO: {
                BizPhotographyWork entity = photographyMapper.selectById(id);
                if (entity == null) throw new RuntimeException("摄影作品不存在");
                workInfo.put("title", entity.getTitle());
                workInfo.put("platform", entity.getPlatform());
                workInfo.put("photoCount", entity.getPhotoCount());
                workInfo.put("readCount", entity.getReadCount());
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("articleLink", entity.getArticleLink());
                workInfo.put("isPublished", entity.getIsPublished());
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setRemark(entity.getRemark());
                break;
            }
            case VIDEO: {
                BizVideoWork entity = videoMapper.selectById(id);
                if (entity == null) throw new RuntimeException("视频作品不存在");
                workInfo.put("videoTitle", entity.getVideoTitle());
                workInfo.put("category", entity.getCategory());
                workInfo.put("videoDurationSec", entity.getVideoDurationSec());
                workInfo.put("likeCount", entity.getLikeCount());
                workInfo.put("reprintStatus", entity.getReprintStatus());
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("videoLink", entity.getVideoLink());
                workInfo.put("isPublished", entity.getIsPublished());
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setRemark(entity.getRemark());
                break;
            }
            case VISUAL: {
                BizVisualWork entity = visualMapper.selectById(id);
                if (entity == null) throw new RuntimeException("视觉作品不存在");
                workInfo.put("title", entity.getTitle());
                workInfo.put("category", entity.getCategory());
                workInfo.put("authorCount", entity.getAuthorCount());
                if (canSeeAmount) {
                    workInfo.put("authorAvgFee", entity.getAuthorAvgFee());
                }
                workInfo.put("publishDate", entity.getPublishDate());
                workInfo.put("articleLink", entity.getArticleLink());
                workInfo.put("isPublished", entity.getIsPublished());
                totalAmount = entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO;
                vo.setRemark(entity.getRemark());
                break;
            }
            default:
                throw new RuntimeException("不支持的作品类型: " + workType);
        }

        vo.setWorkInfo(workInfo);
        vo.setTotalAmount(canSeeAmount ? totalAmount : null);
        vo.setStatus("SUBMITTED");

        // Load contributors
        QueryWrapper<BizWorkContributor> cQuery = new QueryWrapper<>();
        cQuery.eq("work_type", workType).eq("work_id", id);
        List<BizWorkContributor> contributors = contributorMapper.selectList(cQuery);

        List<FeeDetailVO.ContributorVO> contributorVOs = contributors.stream().map(c -> {
            FeeDetailVO.ContributorVO cvo = new FeeDetailVO.ContributorVO();
            cvo.setUserId(c.getUserId());
            cvo.setRoleInWork(c.getRoleInWork());
            cvo.setFeeAmount(canSeeAmount ? c.getFeeAmount() : null);

            // Look up real name
            SysUser user = sysUserMapper.selectById(c.getUserId());
            cvo.setRealName(user != null ? user.getRealName() : "未知");

            return cvo;
        }).collect(Collectors.toList());

        vo.setContributors(contributorVOs);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFee(Long id, String workType, FeeSubmitDTO dto, Long operatorId,
                          String operatorName, String operatorRole, Integer roleCode) {
        if (!canManageFeeAmount(roleCode)) {
            throw new RuntimeException("只有秘书长可以修改稿费任务和金额");
        }
        if (!workType.equals(dto.getWorkType())) {
            throw new RuntimeException("作品类型不能变更");
        }

        updateWorkInfo(workType, id, dto.getWorkInfo());

        QueryWrapper<BizWorkContributor> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("work_type", workType).eq("work_id", id);
        contributorMapper.delete(deleteQuery);

        BigDecimal totalAmount = BigDecimal.ZERO;
        if (dto.getContributors() != null) {
            for (FeeSubmitDTO.ContributorDTO c : dto.getContributors()) {
                BizWorkContributor contributor = new BizWorkContributor();
                contributor.setWorkType(workType);
                contributor.setWorkId(id);
                contributor.setUserId(c.getUserId());
                contributor.setRoleInWork(c.getRoleInWork());
                contributor.setFeeAmount(c.getFeeAmount() != null ? c.getFeeAmount() : BigDecimal.ZERO);
                contributor.setRemark(c.getRemark());
                contributorMapper.insert(contributor);
                totalAmount = totalAmount.add(contributor.getFeeAmount());
            }
        }
        updateWorkTotalAmount(workType, id, totalAmount);

        operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                "FEE", id, String.format("修改稿费任务 [%s] id=%d，总金额 %.2f", workType, id, totalAmount), "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditFee(Long id, String workType, FeeAuditDTO dto, Long auditorId,
                         String auditorName, String auditorRole) {
        String action = Boolean.TRUE.equals(dto.getApproved()) ? "审核通过" : "审核驳回";
        String opinion = dto.getOpinion() != null ? dto.getOpinion() : "";

        // Store audit result in the remark field of the work entity
        String auditRemark = String.format("[%s] %s: %s | %s",
                auditorName, action, opinion,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        switch (workType) {
            case EDITORIAL: {
                BizEditorialArticle entity = editorialMapper.selectById(id);
                if (entity == null) throw new RuntimeException("稿件不存在");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizEditorialArticle update = new BizEditorialArticle();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                editorialMapper.updateById(update);
                break;
            }
            case PHOTO: {
                BizPhotographyWork entity = photographyMapper.selectById(id);
                if (entity == null) throw new RuntimeException("摄影作品不存在");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizPhotographyWork update = new BizPhotographyWork();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                photographyMapper.updateById(update);
                break;
            }
            case VIDEO: {
                BizVideoWork entity = videoMapper.selectById(id);
                if (entity == null) throw new RuntimeException("视频作品不存在");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizVideoWork update = new BizVideoWork();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                videoMapper.updateById(update);
                break;
            }
            case VISUAL: {
                BizVisualWork entity = visualMapper.selectById(id);
                if (entity == null) throw new RuntimeException("视觉作品不存在");
                String existingRemark = entity.getRemark() != null ? entity.getRemark() : "";
                BizVisualWork update = new BizVisualWork();
                update.setId(id);
                update.setRemark(existingRemark + "\n" + auditRemark);
                visualMapper.updateById(update);
                break;
            }
            default:
                throw new RuntimeException("不支持的作品类型: " + workType);
        }

        operationLogService.log(auditorId, auditorName, auditorRole, "AUDIT",
                "FEE", id, String.format("%s [%s] id=%d: %s", action, workType, id, opinion), "");

        log.info("Fee audited: type={}, id={}, approved={}, by={}", workType, id, dto.getApproved(), auditorId);
    }

    @Override
    public void exportFee(String deptCode, String month, Long operatorId, Integer roleCode, HttpServletResponse response) {
        if (!canManageFeeAmount(roleCode)) {
            throw new RuntimeException("只有秘书长可以导出稿费表");
        }
        try {
            // Set response headers for Excel download
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder_encode("稿费清单_" + (month != null ? month : "全部") + ".xlsx");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            // Collect all fee items for export
            List<FeeListVO> allItems = new ArrayList<>();
            allItems.addAll(queryEditorialList(month));
            allItems.addAll(queryPhotographyList(month));
            allItems.addAll(queryVideoList(month));
            allItems.addAll(queryVisualList(month));
            Set<String> allowedDeptCodes = allowedFeeDeptCodes(operatorId, roleCode);
            if (StringUtils.hasText(deptCode)) {
                allowedDeptCodes = Collections.singleton(deptCode);
            }
            Set<String> finalAllowedDeptCodes = allowedDeptCodes;
            allItems = allItems.stream()
                    .filter(item -> finalAllowedDeptCodes.contains(workTypeToDeptCode(item.getWorkType())))
                    .collect(Collectors.toList());

            // Sort by createTime desc
            allItems.sort((a, b) -> {
                if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                if (a.getCreateTime() == null) return 1;
                if (b.getCreateTime() == null) return -1;
                return b.getCreateTime().compareTo(a.getCreateTime());
            });

            List<List<String>> head = Arrays.asList(
                    Collections.singletonList("作品类型"),
                    Collections.singletonList("标题"),
                    Collections.singletonList("部门"),
                    Collections.singletonList("金额"),
                    Collections.singletonList("状态"),
                    Collections.singletonList("提交/关联人员"),
                    Collections.singletonList("创建时间")
            );
            List<List<Object>> data = allItems.stream()
                    .map(item -> Arrays.asList(
                            (Object) workTypeDisplayName(item.getWorkType()),
                            item.getTitle(),
                            item.getDeptName(),
                            item.getTotalAmount(),
                            item.getStatus(),
                            item.getSubmitterName(),
                            item.getCreateTime()
                    ))
                    .collect(Collectors.toList());
            EasyExcel.write(response.getOutputStream()).head(head).sheet("稿费明细").doWrite(data);

        } catch (Exception e) {
            log.error("Failed to export fee data", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    // ======================== Private helpers ========================

    private Long createEditorialWork(Map<String, Object> info) {
        BizEditorialArticle entity = new BizEditorialArticle();
        entity.setTitle(getStringVal(info, "title"));
        entity.setPlatform(getStringVal(info, "platform"));
        entity.setCategory(getStringVal(info, "category"));
        entity.setWordCount(getIntVal(info, "wordCount"));
        entity.setReadCount(getIntVal(info, "readCount"));
        entity.setPublishDate(getLocalDateVal(info, "publishDate"));
        entity.setArticleLink(getStringVal(info, "articleLink"));
        entity.setIsPublished(getIntVal(info, "isPublished"));
        entity.setRemark(getStringVal(info, "remark"));
        editorialMapper.insert(entity);
        return entity.getId();
    }

    private Long createPhotographyWork(Map<String, Object> info) {
        BizPhotographyWork entity = new BizPhotographyWork();
        entity.setTitle(getStringVal(info, "title"));
        entity.setPlatform(getStringVal(info, "platform"));
        entity.setPhotoCount(getIntVal(info, "photoCount"));
        entity.setReadCount(getIntVal(info, "readCount"));
        entity.setPublishDate(getLocalDateVal(info, "publishDate"));
        entity.setArticleLink(getStringVal(info, "articleLink"));
        entity.setIsPublished(getIntVal(info, "isPublished"));
        entity.setRemark(getStringVal(info, "remark"));
        photographyMapper.insert(entity);
        return entity.getId();
    }

    private Long createVideoWork(Map<String, Object> info) {
        BizVideoWork entity = new BizVideoWork();
        entity.setVideoTitle(getStringVal(info, "videoTitle"));
        entity.setCategory(getStringVal(info, "category"));
        entity.setVideoDurationSec(getIntVal(info, "videoDurationSec"));
        entity.setLikeCount(getIntVal(info, "likeCount"));
        entity.setReprintStatus(getStringVal(info, "reprintStatus"));
        entity.setPublishDate(getLocalDateVal(info, "publishDate"));
        entity.setVideoLink(getStringVal(info, "videoLink"));
        entity.setIsPublished(getIntVal(info, "isPublished"));
        entity.setRemark(getStringVal(info, "remark"));
        videoMapper.insert(entity);
        return entity.getId();
    }

    private Long createVisualWork(Map<String, Object> info) {
        BizVisualWork entity = new BizVisualWork();
        entity.setTitle(getStringVal(info, "title"));
        entity.setCategory(getStringVal(info, "category"));
        entity.setAuthorCount(getIntVal(info, "authorCount"));
        entity.setPublishDate(getLocalDateVal(info, "publishDate"));
        entity.setArticleLink(getStringVal(info, "articleLink"));
        entity.setIsPublished(getIntVal(info, "isPublished"));
        entity.setRemark(getStringVal(info, "remark"));
        visualMapper.insert(entity);
        return entity.getId();
    }

    private void updateWorkTotalAmount(String workType, Long workId, BigDecimal totalAmount) {
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
                visualMapper.updateById(update);
                break;
            }
        }
    }

    private void updateWorkInfo(String workType, Long workId, Map<String, Object> info) {
        switch (workType) {
            case EDITORIAL: {
                if (editorialMapper.selectById(workId) == null) throw new RuntimeException("稿件不存在");
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
                update.setRemark(getStringVal(info, "remark"));
                editorialMapper.updateById(update);
                break;
            }
            case PHOTO: {
                if (photographyMapper.selectById(workId) == null) throw new RuntimeException("摄影作品不存在");
                BizPhotographyWork update = new BizPhotographyWork();
                update.setId(workId);
                update.setTitle(getStringVal(info, "title"));
                update.setPlatform(getStringVal(info, "platform"));
                update.setPhotoCount(getIntVal(info, "photoCount"));
                update.setReadCount(getIntVal(info, "readCount"));
                update.setPublishDate(getLocalDateVal(info, "publishDate"));
                update.setArticleLink(getStringVal(info, "articleLink"));
                update.setIsPublished(getIntVal(info, "isPublished"));
                update.setRemark(getStringVal(info, "remark"));
                photographyMapper.updateById(update);
                break;
            }
            case VIDEO: {
                if (videoMapper.selectById(workId) == null) throw new RuntimeException("视频作品不存在");
                BizVideoWork update = new BizVideoWork();
                update.setId(workId);
                update.setVideoTitle(getStringVal(info, "videoTitle"));
                update.setCategory(getStringVal(info, "category"));
                update.setVideoDurationSec(getIntVal(info, "videoDurationSec"));
                update.setLikeCount(getIntVal(info, "likeCount"));
                update.setReprintStatus(getStringVal(info, "reprintStatus"));
                update.setPublishDate(getLocalDateVal(info, "publishDate"));
                update.setVideoLink(getStringVal(info, "videoLink"));
                update.setIsPublished(getIntVal(info, "isPublished"));
                update.setRemark(getStringVal(info, "remark"));
                videoMapper.updateById(update);
                break;
            }
            case VISUAL: {
                if (visualMapper.selectById(workId) == null) throw new RuntimeException("视觉作品不存在");
                BizVisualWork update = new BizVisualWork();
                update.setId(workId);
                update.setTitle(getStringVal(info, "title"));
                update.setCategory(getStringVal(info, "category"));
                update.setAuthorCount(getIntVal(info, "authorCount"));
                update.setAuthorAvgFee(getBigDecimalVal(info, "authorAvgFee"));
                update.setPublishDate(getLocalDateVal(info, "publishDate"));
                update.setArticleLink(getStringVal(info, "articleLink"));
                update.setIsPublished(getIntVal(info, "isPublished"));
                update.setRemark(getStringVal(info, "remark"));
                visualMapper.updateById(update);
                break;
            }
            default:
                throw new RuntimeException("不支持的作品类型: " + workType);
        }
    }

    private void ensureCanAccessWorkType(Long userId, Integer roleCode, String workType) {
        String deptCode = workTypeToDeptCode(workType);
        if (!allowedFeeDeptCodes(userId, roleCode).contains(deptCode)) {
            throw new RuntimeException("不能访问其他部门的稿费任务");
        }
    }

    private Set<String> allowedFeeDeptCodes(Long userId, Integer roleCode) {
        if (isGlobalTaskViewer(roleCode)) {
            return new HashSet<>(Arrays.asList("EDITORIAL", "PHOTOGRAPHY", "VIDEO", "VISUAL"));
        }
        return getUserDeptCodes(userId);
    }

    private boolean isGlobalTaskViewer(Integer roleCode) {
        return roleCode != null && (roleCode == 0 || roleCode == 1 || roleCode == 2 || roleCode == 3);
    }

    private boolean canManageFeeAmount(Integer roleCode) {
        return roleCode != null && (roleCode == 0 || roleCode == 3);
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
            default:
                throw new RuntimeException("不支持的作品类型: " + workType);
        }
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
            default:
                return workType;
        }
    }

    private List<FeeListVO> queryEditorialList(String month) {
        QueryWrapper<BizEditorialArticle> query = new QueryWrapper<>();
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
            vo.setTotalAmount(a.getTotalAmount());
            vo.setStatus("SUBMITTED");
            vo.setCreateTime(a.getCreateTime());
            // Resolve submitter from first contributor
            resolveSubmitter(vo, EDITORIAL, a.getId());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<FeeListVO> queryPhotographyList(String month) {
        QueryWrapper<BizPhotographyWork> query = new QueryWrapper<>();
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
            vo.setTotalAmount(w.getTotalAmount());
            vo.setStatus("SUBMITTED");
            vo.setCreateTime(w.getCreateTime());
            resolveSubmitter(vo, PHOTO, w.getId());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<FeeListVO> queryVideoList(String month) {
        QueryWrapper<BizVideoWork> query = new QueryWrapper<>();
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
            vo.setTotalAmount(w.getTotalAmount());
            vo.setStatus("SUBMITTED");
            vo.setCreateTime(w.getCreateTime());
            resolveSubmitter(vo, VIDEO, w.getId());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<FeeListVO> queryVisualList(String month) {
        QueryWrapper<BizVisualWork> query = new QueryWrapper<>();
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
            vo.setTotalAmount(w.getTotalAmount());
            vo.setStatus("SUBMITTED");
            vo.setCreateTime(w.getCreateTime());
            resolveSubmitter(vo, VISUAL, w.getId());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * Resolve the submitter name from the first contributor of a work.
     */
    private void resolveSubmitter(FeeListVO vo, String workType, Long workId) {
        QueryWrapper<BizWorkContributor> cQuery = new QueryWrapper<>();
        cQuery.eq("work_type", workType).eq("work_id", workId).last("LIMIT 1");
        BizWorkContributor contributor = contributorMapper.selectOne(cQuery);
        if (contributor != null) {
            SysUser user = sysUserMapper.selectById(contributor.getUserId());
            vo.setSubmitterName(user != null ? user.getRealName() : "未知");
        } else {
            vo.setSubmitterName("未知");
        }
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
