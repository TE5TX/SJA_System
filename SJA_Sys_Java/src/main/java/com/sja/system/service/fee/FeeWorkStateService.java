package com.sja.system.service.fee;

import com.sja.system.common.ApiException;
import com.sja.system.dal.dataobject.BizEditorialArticle;
import com.sja.system.dal.dataobject.BizMonthlySubsidy;
import com.sja.system.dal.dataobject.BizPhotographyWork;
import com.sja.system.dal.dataobject.BizVideoWork;
import com.sja.system.dal.dataobject.BizVisualWork;
import com.sja.system.dal.mysql.BizEditorialArticleMapper;
import com.sja.system.dal.mysql.BizMonthlySubsidyMapper;
import com.sja.system.dal.mysql.BizPhotographyWorkMapper;
import com.sja.system.dal.mysql.BizVideoWorkMapper;
import com.sja.system.dal.mysql.BizVisualWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 屏蔽五类稿费作品在审核状态、有效状态和备注字段上的持久化差异。
 */
@Service
@RequiredArgsConstructor
// 202609201644 - 将五类作品的审核状态持久化从审核流程中独立出来。
public class FeeWorkStateService {

    private final BizEditorialArticleMapper editorialMapper;
    private final BizPhotographyWorkMapper photographyMapper;
    private final BizVideoWorkMapper videoMapper;
    private final BizVisualWorkMapper visualMapper;
    private final BizMonthlySubsidyMapper subsidyMapper;

    public String auditStatus(String workType, Long id) {
        return switch (FeeWorkType.require(workType)) {
            case EDITORIAL -> normalize(requireEditorial(id).getAuditStatus());
            case PHOTO -> normalize(requirePhoto(id).getAuditStatus());
            case VIDEO -> normalize(requireVideo(id).getAuditStatus());
            case VISUAL -> normalize(requireVisual(id).getAuditStatus());
            case SUBSIDY -> normalize(requireSubsidy(id).getAuditStatus());
        };
    }

    public void updateAudit(String workType, Long id, String status, String appendedRemark) {
        switch (FeeWorkType.require(workType)) {
            case EDITORIAL -> {
                BizEditorialArticle current = requireEditorial(id);
                BizEditorialArticle update = new BizEditorialArticle();
                update.setId(id);
                update.setAuditStatus(status);
                if (appendedRemark != null) update.setRemark(append(current.getRemark(), appendedRemark));
                editorialMapper.updateById(update);
            }
            case PHOTO -> {
                BizPhotographyWork current = requirePhoto(id);
                BizPhotographyWork update = new BizPhotographyWork();
                update.setId(id);
                update.setAuditStatus(status);
                if (appendedRemark != null) update.setRemark(append(current.getRemark(), appendedRemark));
                photographyMapper.updateById(update);
            }
            case VIDEO -> {
                BizVideoWork current = requireVideo(id);
                BizVideoWork update = new BizVideoWork();
                update.setId(id);
                update.setAuditStatus(status);
                if (appendedRemark != null) update.setRemark(append(current.getRemark(), appendedRemark));
                videoMapper.updateById(update);
            }
            case VISUAL -> {
                BizVisualWork current = requireVisual(id);
                BizVisualWork update = new BizVisualWork();
                update.setId(id);
                update.setAuditStatus(status);
                if (appendedRemark != null) update.setRemark(append(current.getRemark(), appendedRemark));
                visualMapper.updateById(update);
            }
            case SUBSIDY -> {
                BizMonthlySubsidy current = requireSubsidy(id);
                BizMonthlySubsidy update = new BizMonthlySubsidy();
                update.setId(id);
                update.setAuditStatus(status);
                if (appendedRemark != null) update.setRemark(append(current.getRemark(), appendedRemark));
                subsidyMapper.updateById(update);
            }
        }
    }

    public void appendRemark(String workType, Long id, String remark) {
        String currentStatus = auditStatus(workType, id);
        updateAudit(workType, id, currentStatus, remark);
    }

    public void updateValidity(String workType, Long id, int valid) {
        auditStatus(workType, id);
        switch (FeeWorkType.require(workType)) {
            case EDITORIAL -> {
                BizEditorialArticle update = new BizEditorialArticle(); update.setId(id); update.setIsValid(valid);
                editorialMapper.updateById(update);
            }
            case PHOTO -> {
                BizPhotographyWork update = new BizPhotographyWork(); update.setId(id); update.setIsValid(valid);
                photographyMapper.updateById(update);
            }
            case VIDEO -> {
                BizVideoWork update = new BizVideoWork(); update.setId(id); update.setIsValid(valid);
                videoMapper.updateById(update);
            }
            case VISUAL -> {
                BizVisualWork update = new BizVisualWork(); update.setId(id); update.setIsValid(valid);
                visualMapper.updateById(update);
            }
            case SUBSIDY -> {
                BizMonthlySubsidy update = new BizMonthlySubsidy(); update.setId(id); update.setIsValid(valid);
                subsidyMapper.updateById(update);
            }
        }
    }

    private BizEditorialArticle requireEditorial(Long id) {
        BizEditorialArticle entity = editorialMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("稿件不存在或已失效");
        return entity;
    }

    private BizPhotographyWork requirePhoto(Long id) {
        BizPhotographyWork entity = photographyMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("摄影作品不存在或已失效");
        return entity;
    }

    private BizVideoWork requireVideo(Long id) {
        BizVideoWork entity = videoMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("视频作品不存在或已失效");
        return entity;
    }

    private BizVisualWork requireVisual(Long id) {
        BizVisualWork entity = visualMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("视觉作品不存在或已失效");
        return entity;
    }

    private BizMonthlySubsidy requireSubsidy(Long id) {
        BizMonthlySubsidy entity = subsidyMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getIsValid(), 1)) throw ApiException.notFound("月度补贴任务不存在或已失效");
        return entity;
    }

    private String append(String current, String value) {
        return StringUtils.hasText(current) ? current + "\n" + value : value;
    }

    private String normalize(String status) {
        return StringUtils.hasText(status) ? status : "SUBMITTED";
    }
}
