package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.dal.dataobject.BizPhotographyWork;
import com.sja.system.dal.mysql.BizPhotographyWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 摄影作品持久化适配器。 */
@Component
@RequiredArgsConstructor
// 202609201644 - 将摄影作品字段映射和重复判断封装为独立适配器。
public class PhotographyFeeWorkAdapter implements FeeWorkAdapter {

    private final BizPhotographyWorkMapper mapper;

    @Override public FeeWorkType supports() { return FeeWorkType.PHOTO; }

    @Override public Long create(Map<String, Object> info, Long submitterId) {
        BizPhotographyWork entity = values(new BizPhotographyWork(), info);
        entity.setSubmitterId(submitterId); mapper.insert(entity); return entity.getId();
    }

    @Override public void update(Long id, Map<String, Object> info) {
        if (mapper.selectById(id) == null) throw ApiException.notFound("摄影作品不存在");
        BizPhotographyWork entity = values(new BizPhotographyWork(), info); entity.setId(id); mapper.updateById(entity);
        if (!Integer.valueOf(1).equals(FeeValueReader.integer(info, "isPublished"))) {
            UpdateWrapper<BizPhotographyWork> update = new UpdateWrapper<>();
            update.eq("id", id).set("publish_date", null); mapper.update(null, update);
        }
    }

    @Override public void updateTotal(Long id, BigDecimal total, int contributorCount) {
        BizPhotographyWork update = new BizPhotographyWork(); update.setId(id); update.setTotalAmount(total); mapper.updateById(update);
    }

    @Override public Map<String, Object> preserveRestrictedFields(Long id, Map<String, Object> submitted) {
        BizPhotographyWork current = mapper.selectById(id);
        if (current == null) throw ApiException.notFound("摄影作品不存在");
        Map<String, Object> result = new LinkedHashMap<>(submitted);
        result.put("isSpecialNode", current.getIsSpecialNode()); result.put("specialMultiplier", current.getSpecialMultiplier());
        return result;
    }

    @Override public boolean existsDuplicate(Map<String, Object> info, List<FeeSubmitDTO.ContributorDTO> contributors, Long excludedId) {
        QueryWrapper<BizPhotographyWork> query = new QueryWrapper<>(); query.eq("is_valid", 1);
        FeeValueReader.applyIdentity(query, "article_link", FeeValueReader.string(info, "articleLink"),
                "title", FeeValueReader.string(info, "title"), "platform", FeeValueReader.string(info, "platform"),
                "publish_date", FeeValueReader.date(info, "publishDate"));
        if (excludedId != null) query.ne("id", excludedId);
        return mapper.selectCount(query) > 0;
    }

    private BizPhotographyWork values(BizPhotographyWork entity, Map<String, Object> info) {
        entity.setTitle(FeeValueReader.string(info, "title")); entity.setPlatform(FeeValueReader.string(info, "platform"));
        entity.setCategory(FeeValueReader.string(info, "category")); entity.setPhotoCount(FeeValueReader.integer(info, "photoCount"));
        entity.setReadCount(FeeValueReader.integer(info, "readCount")); entity.setPublishDate(FeeValueReader.date(info, "publishDate"));
        entity.setArticleLink(FeeValueReader.string(info, "articleLink")); entity.setIsPublished(FeeValueReader.integer(info, "isPublished"));
        entity.setIsSpecialNode(FeeValueReader.integer(info, "isSpecialNode"));
        entity.setSpecialMultiplier(FeeValueReader.specialMultiplier(info)); entity.setRemark(FeeValueReader.string(info, "remark"));
        return entity;
    }
}
