package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.entity.BizVisualWork;
import com.sja.system.mapper.BizVisualWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 视觉作品持久化适配器。 */
@Component
@RequiredArgsConstructor
// 202609201644 - 将视觉作品字段映射、金额汇总和重复判断封装为独立适配器。
public class VisualFeeWorkAdapter implements FeeWorkAdapter {

    private final BizVisualWorkMapper mapper;

    @Override public FeeWorkType supports() { return FeeWorkType.VISUAL; }

    @Override public Long create(Map<String, Object> info, Long submitterId) {
        BizVisualWork entity = values(new BizVisualWork(), info); entity.setSubmitterId(submitterId);
        entity.setAuthorCount(FeeValueReader.integer(info, "authorCount")); mapper.insert(entity); return entity.getId();
    }

    @Override public void update(Long id, Map<String, Object> info) {
        if (mapper.selectById(id) == null) throw ApiException.notFound("视觉作品不存在");
        BizVisualWork entity = values(new BizVisualWork(), info); entity.setId(id); mapper.updateById(entity);
        if (!Integer.valueOf(1).equals(FeeValueReader.integer(info, "isPublished"))) {
            UpdateWrapper<BizVisualWork> update = new UpdateWrapper<>();
            update.eq("id", id).set("publish_date", null); mapper.update(null, update);
        }
    }

    @Override public void updateTotal(Long id, BigDecimal total, int count) {
        BizVisualWork update = new BizVisualWork(); update.setId(id); update.setTotalAmount(total); update.setAuthorCount(count);
        update.setAuthorAvgFee(count > 0 ? total.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        mapper.updateById(update);
    }

    @Override public Map<String, Object> preserveRestrictedFields(Long id, Map<String, Object> submitted) {
        BizVisualWork current = mapper.selectById(id);
        if (current == null) throw ApiException.notFound("视觉作品不存在");
        Map<String, Object> result = new LinkedHashMap<>(submitted);
        result.put("isSpecialNode", current.getIsSpecialNode()); result.put("specialMultiplier", current.getSpecialMultiplier());
        result.put("authorAvgFee", current.getAuthorAvgFee()); return result;
    }

    @Override public boolean existsDuplicate(Map<String, Object> info, List<FeeSubmitDTO.ContributorDTO> contributors, Long excludedId) {
        QueryWrapper<BizVisualWork> query = new QueryWrapper<>(); query.eq("is_valid", 1);
        FeeValueReader.applyIdentity(query, "article_link", FeeValueReader.string(info, "articleLink"),
                "title", FeeValueReader.string(info, "title"), "category",
                FeeValueReader.visualCategory(FeeValueReader.string(info, "category")), "publish_date", FeeValueReader.date(info, "publishDate"));
        if (excludedId != null) query.ne("id", excludedId);
        return mapper.selectCount(query) > 0;
    }

    private BizVisualWork values(BizVisualWork entity, Map<String, Object> info) {
        entity.setTitle(FeeValueReader.string(info, "title"));
        entity.setCategory(FeeValueReader.visualCategory(FeeValueReader.string(info, "category")));
        entity.setPublishDate(FeeValueReader.date(info, "publishDate")); entity.setArticleLink(FeeValueReader.string(info, "articleLink"));
        entity.setIsPublished(FeeValueReader.integer(info, "isPublished")); entity.setIsSpecialNode(FeeValueReader.integer(info, "isSpecialNode"));
        entity.setSpecialMultiplier(FeeValueReader.specialMultiplier(info)); entity.setRemark(FeeValueReader.string(info, "remark"));
        return entity;
    }
}
