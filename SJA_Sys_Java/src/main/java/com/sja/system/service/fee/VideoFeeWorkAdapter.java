package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.dal.dataobject.BizVideoWork;
import com.sja.system.dal.mysql.BizVideoWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 视频作品持久化适配器。 */
@Component
@RequiredArgsConstructor
// 202609201644 - 将视频作品字段映射和重复判断封装为独立适配器。
public class VideoFeeWorkAdapter implements FeeWorkAdapter {

    private final BizVideoWorkMapper mapper;

    @Override public FeeWorkType supports() { return FeeWorkType.VIDEO; }

    @Override public Long create(Map<String, Object> info, Long submitterId) {
        BizVideoWork entity = values(new BizVideoWork(), info); entity.setSubmitterId(submitterId);
        mapper.insert(entity); return entity.getId();
    }

    @Override public void update(Long id, Map<String, Object> info) {
        if (mapper.selectById(id) == null) throw ApiException.notFound("视频作品不存在");
        BizVideoWork entity = values(new BizVideoWork(), info); entity.setId(id); mapper.updateById(entity);
    }

    @Override public void assignCreationDateToUnpublishedWork(Long id) {
        // 202609281554 - 视频未发布作品使用数据库创建日期，编辑时保持原创建日期不变。
        UpdateWrapper<BizVideoWork> update = new UpdateWrapper<>();
        update.eq("id", id).setSql("publish_date = DATE(create_time)");
        mapper.update(null, update);
    }

    @Override public void updateTotal(Long id, BigDecimal total, int contributorCount) {
        BizVideoWork update = new BizVideoWork(); update.setId(id); update.setTotalAmount(total); mapper.updateById(update);
    }

    @Override public Map<String, Object> preserveRestrictedFields(Long id, Map<String, Object> submitted) {
        BizVideoWork current = mapper.selectById(id);
        if (current == null) throw ApiException.notFound("视频作品不存在");
        Map<String, Object> result = new LinkedHashMap<>(submitted);
        result.put("isSpecialNode", current.getIsSpecialNode()); result.put("specialMultiplier", current.getSpecialMultiplier());
        return result;
    }

    @Override public boolean existsDuplicate(Map<String, Object> info, List<FeeSubmitDTO.ContributorDTO> contributors, Long excludedId) {
        QueryWrapper<BizVideoWork> query = new QueryWrapper<>(); query.eq("is_valid", 1);
        FeeValueReader.applyIdentity(query, "video_link", FeeValueReader.string(info, "videoLink"),
                "video_title", FeeValueReader.string(info, "videoTitle"), "category", FeeValueReader.string(info, "category"),
                "publish_date", FeeValueReader.date(info, "publishDate"));
        if (excludedId != null) query.ne("id", excludedId);
        return mapper.selectCount(query) > 0;
    }

    private BizVideoWork values(BizVideoWork entity, Map<String, Object> info) {
        entity.setVideoTitle(FeeValueReader.string(info, "videoTitle")); entity.setCategory(FeeValueReader.string(info, "category"));
        entity.setVideoDurationSec(FeeValueReader.integer(info, "videoDurationSec")); entity.setLikeCount(FeeValueReader.integer(info, "likeCount"));
        entity.setReprintStatus(Objects.equals(FeeValueReader.integer(info, "isReposted"), 1) ? "官方媒体转载" : "无");
        entity.setIsComplexEdit(FeeValueReader.integer(info, "isComplexEdit")); entity.setPublishDate(FeeValueReader.date(info, "publishDate"));
        entity.setVideoLink(FeeValueReader.string(info, "videoLink")); entity.setIsPublished(FeeValueReader.integer(info, "isPublished"));
        entity.setIsSpecialNode(FeeValueReader.integer(info, "isSpecialNode")); entity.setSpecialMultiplier(FeeValueReader.specialMultiplier(info));
        entity.setRemark(FeeValueReader.string(info, "remark")); return entity;
    }
}
