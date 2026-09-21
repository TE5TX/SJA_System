package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.dal.dataobject.BizMonthlySubsidy;
import com.sja.system.dal.dataobject.BizWorkContributor;
import com.sja.system.dal.mysql.BizMonthlySubsidyMapper;
import com.sja.system.dal.mysql.BizWorkContributorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 月度补贴持久化适配器。 */
@Component
@RequiredArgsConstructor
// 202609201644 - 将月度补贴字段映射和同月重复人员判断封装为独立适配器。
public class SubsidyFeeWorkAdapter implements FeeWorkAdapter {

    private final BizMonthlySubsidyMapper mapper;
    private final BizWorkContributorMapper contributorMapper;

    @Override public FeeWorkType supports() { return FeeWorkType.SUBSIDY; }

    @Override public Long create(Map<String, Object> info, Long submitterId) {
        BizMonthlySubsidy entity = values(new BizMonthlySubsidy(), info); entity.setSubmitterId(submitterId);
        mapper.insert(entity); return entity.getId();
    }

    @Override public void update(Long id, Map<String, Object> info) {
        if (mapper.selectById(id) == null) throw ApiException.notFound("月度补贴任务不存在");
        BizMonthlySubsidy entity = values(new BizMonthlySubsidy(), info); entity.setId(id); mapper.updateById(entity);
    }

    @Override public void updateTotal(Long id, BigDecimal total, int contributorCount) {
        BizMonthlySubsidy update = new BizMonthlySubsidy(); update.setId(id); update.setTotalAmount(total); mapper.updateById(update);
    }

    @Override public Map<String, Object> preserveRestrictedFields(Long id, Map<String, Object> submitted) {
        if (mapper.selectById(id) == null) throw ApiException.notFound("月度补贴任务不存在");
        return new LinkedHashMap<>(submitted);
    }

    @Override public boolean existsDuplicate(Map<String, Object> info, List<FeeSubmitDTO.ContributorDTO> contributors, Long excludedId) {
        if (contributors == null || contributors.isEmpty()) return false;
        QueryWrapper<BizMonthlySubsidy> taskQuery = new QueryWrapper<>();
        taskQuery.select("id").eq("is_valid", 1)
                .eq("category", FeeValueReader.string(info, "category"))
                .eq("subsidy_month", FeeValueReader.string(info, "subsidyMonth"));
        if (excludedId != null) taskQuery.ne("id", excludedId);
        List<Long> workIds = mapper.selectList(taskQuery).stream().map(BizMonthlySubsidy::getId).toList();
        if (workIds.isEmpty()) return false;
        Set<Long> userIds = contributors.stream().map(FeeSubmitDTO.ContributorDTO::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (userIds.isEmpty()) return false;
        QueryWrapper<BizWorkContributor> contributorQuery = new QueryWrapper<>();
        contributorQuery.eq("work_type", FeeWorkType.SUBSIDY.code())
                .in("work_id", workIds).in("user_id", userIds);
        return contributorMapper.selectCount(contributorQuery) > 0;
    }

    private BizMonthlySubsidy values(BizMonthlySubsidy entity, Map<String, Object> info) {
        entity.setTitle(FeeValueReader.string(info, "title")); entity.setCategory(FeeValueReader.string(info, "category"));
        entity.setSubsidyMonth(FeeValueReader.string(info, "subsidyMonth")); entity.setRemark(FeeValueReader.string(info, "remark"));
        return entity;
    }
}
