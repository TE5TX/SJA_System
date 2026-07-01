package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    private final OperationLogService operationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFee(Long userId, String username, String realName, String deptCode, FeeSubmitDTO dto) {
        String workType = dto.getWorkType();
        Map<String, Object> workInfo = dto.getWorkInfo();
        List<FeeSubmitDTO.ContributorDTO> contributors = dto.getContributors();

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
                contributor.setFeeAmount(c.getFeeAmount());
                contributor.setRemark(c.getRemark());
                contributorMapper.insert(contributor);

                if (c.getFeeAmount() != null) {
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
                                            Integer pageNum, Integer pageSize) {
        int pn = pageNum != null ? pageNum : 1;
        int ps = pageSize != null ? pageSize : 10;

        List<FeeListVO> allItems = new ArrayList<>();

        // Query each work type and combine results
        allItems.addAll(queryEditorialList(month));
        allItems.addAll(queryPhotographyList(month));
        allItems.addAll(queryVideoList(month));
        allItems.addAll(queryVisualList(month));

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
    public FeeDetailVO getFeeDetail(Long id, String workType) {
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
                workInfo.put("authorAvgFee", entity.getAuthorAvgFee());
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
        vo.setTotalAmount(totalAmount);
        vo.setStatus("SUBMITTED");

        // Load contributors
        QueryWrapper<BizWorkContributor> cQuery = new QueryWrapper<>();
        cQuery.eq("work_type", workType).eq("work_id", id);
        List<BizWorkContributor> contributors = contributorMapper.selectList(cQuery);

        List<FeeDetailVO.ContributorVO> contributorVOs = contributors.stream().map(c -> {
            FeeDetailVO.ContributorVO cvo = new FeeDetailVO.ContributorVO();
            cvo.setUserId(c.getUserId());
            cvo.setRoleInWork(c.getRoleInWork());
            cvo.setFeeAmount(c.getFeeAmount());

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
    public void exportFee(String deptCode, String month, HttpServletResponse response) {
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

            // Sort by createTime desc
            allItems.sort((a, b) -> {
                if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                if (a.getCreateTime() == null) return 1;
                if (b.getCreateTime() == null) return -1;
                return b.getCreateTime().compareTo(a.getCreateTime());
            });

            // Write a simple CSV-like output since EasyExcel may not be available
            // In production, use EasyExcel or Apache POI
            StringBuilder sb = new StringBuilder();
            sb.append("作品类型,标题,部门,金额,状态,提交人,创建时间\n");
            for (FeeListVO item : allItems) {
                sb.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                        item.getWorkType(),
                        item.getTitle(),
                        item.getDeptName(),
                        item.getTotalAmount(),
                        item.getStatus(),
                        item.getSubmitterName(),
                        item.getCreateTime()));
            }
            response.getWriter().write(sb.toString());
            response.getWriter().flush();

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
