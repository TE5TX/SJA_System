package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.dal.dataobject.BizEditorialArticle;
import com.sja.system.dal.mysql.BizEditorialArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 采编稿件持久化适配器。 */
@Component
@RequiredArgsConstructor
// 202609201644 - 将采编稿件字段映射和重复判断封装为独立适配器。
public class EditorialFeeWorkAdapter implements FeeWorkAdapter {

    private final BizEditorialArticleMapper mapper;

    @Override public FeeWorkType supports() { return FeeWorkType.EDITORIAL; }

    @Override
    public Long create(Map<String, Object> info, Long submitterId) {
        BizEditorialArticle entity = values(new BizEditorialArticle(), info);
        entity.setSubmitterId(submitterId);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, Map<String, Object> info) {
        if (mapper.selectById(id) == null) throw ApiException.notFound("稿件不存在");
        BizEditorialArticle entity = values(new BizEditorialArticle(), info);
        entity.setId(id);
        mapper.updateById(entity);
    }

    @Override
    public void updateTotal(Long id, BigDecimal total, int contributorCount) {
        BizEditorialArticle update = new BizEditorialArticle();
        update.setId(id); update.setTotalAmount(total); mapper.updateById(update);
    }

    @Override
    public Map<String, Object> preserveRestrictedFields(Long id, Map<String, Object> submitted) {
        BizEditorialArticle current = mapper.selectById(id);
        if (current == null) throw ApiException.notFound("稿件不存在");
        Map<String, Object> result = new LinkedHashMap<>(submitted);
        result.put("isSpecialNode", current.getIsSpecialNode());
        result.put("specialMultiplier", current.getSpecialMultiplier());
        return result;
    }

    @Override
    public boolean existsDuplicate(Map<String, Object> info, List<FeeSubmitDTO.ContributorDTO> contributors,
                                   Long excludedId) {
        QueryWrapper<BizEditorialArticle> query = new QueryWrapper<>();
        query.eq("is_valid", 1);
        FeeValueReader.applyIdentity(query, "article_link", FeeValueReader.string(info, "articleLink"),
                "title", FeeValueReader.string(info, "title"), "platform",
                FeeValueReader.string(info, "platform"), "publish_date", FeeValueReader.date(info, "publishDate"));
        if (excludedId != null) query.ne("id", excludedId);
        return mapper.selectCount(query) > 0;
    }

    private BizEditorialArticle values(BizEditorialArticle entity, Map<String, Object> info) {
        entity.setTitle(FeeValueReader.string(info, "title"));
        entity.setPlatform(FeeValueReader.string(info, "platform"));
        entity.setCategory(FeeValueReader.string(info, "category"));
        entity.setWordCount(FeeValueReader.integer(info, "wordCount"));
        entity.setReadCount(FeeValueReader.integer(info, "readCount"));
        entity.setPublishDate(FeeValueReader.date(info, "publishDate"));
        entity.setArticleLink(FeeValueReader.string(info, "articleLink"));
        entity.setIsPublished(FeeValueReader.integer(info, "isPublished"));
        entity.setIsSpecialNode(FeeValueReader.integer(info, "isSpecialNode"));
        entity.setSpecialMultiplier(FeeValueReader.specialMultiplier(info));
        entity.setRemark(FeeValueReader.string(info, "remark"));
        return entity;
    }

    @Override
    public void assignCreationDateToUnpublishedWork(Long id) {
        // 202609281554 - 采编未发布稿件使用数据库创建日期，避免客户端时间或异步排队造成偏差。
        UpdateWrapper<BizEditorialArticle> update = new UpdateWrapper<>();
        update.eq("id", id).setSql("publish_date = DATE(create_time)");
        mapper.update(null, update);
    }
}
