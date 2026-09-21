package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.entity.BizWorkContributor;
import com.sja.system.mapper.BizWorkContributorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 参与人员稿费明细的替换式保存服务。
 */
@Service
@RequiredArgsConstructor
// 202609201644 - 将参与人员删除、保存及金额汇总从稿费写入流程中独立出来。
public class FeeContributorService {

    private static final int MONEY_SCALE = 1;

    private final BizWorkContributorMapper contributorMapper;

    public BigDecimal replace(String workType, Long workId,
                              List<FeeSubmitDTO.ContributorDTO> contributors,
                              List<BigDecimal> calculatedFees) {
        QueryWrapper<BizWorkContributor> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("work_type", workType).eq("work_id", workId);
        contributorMapper.delete(deleteQuery);
        return insert(workType, workId, contributors, calculatedFees);
    }

    public BigDecimal insert(String workType, Long workId,
                             List<FeeSubmitDTO.ContributorDTO> contributors,
                             List<BigDecimal> calculatedFees) {
        BigDecimal total = BigDecimal.ZERO;
        if (contributors == null || contributors.isEmpty()) return total;
        for (int index = 0; index < contributors.size(); index++) {
            FeeSubmitDTO.ContributorDTO source = contributors.get(index);
            BizWorkContributor entity = new BizWorkContributor();
            entity.setWorkType(workType);
            entity.setWorkId(workId);
            entity.setUserId(source.getUserId());
            FeeWorkType type = FeeWorkType.require(workType);
            entity.setRoleInWork(type == FeeWorkType.EDITORIAL || type == FeeWorkType.PHOTO
                    ? "AUTHOR" : source.getRoleInWork());
            BigDecimal fee = calculatedFees != null && index < calculatedFees.size()
                    ? calculatedFees.get(index) : BigDecimal.ZERO;
            entity.setFeeAmount(normalize(fee));
            entity.setWorkCount(source.getWorkCount());
            entity.setRemark(source.getRemark());
            contributorMapper.insert(entity);
            total = total.add(entity.getFeeAmount());
        }
        return total;
    }

    private BigDecimal normalize(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
