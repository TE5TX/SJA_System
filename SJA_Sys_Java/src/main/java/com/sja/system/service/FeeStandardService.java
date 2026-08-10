package com.sja.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeStandardUpdateDTO;
import com.sja.system.entity.BizFeeStandard;
import com.sja.system.mapper.BizFeeStandardMapper;
import com.sja.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// 202608101042 - 修复了稿费金额、比例和阈值写死在代码中而无法配置的问题。
@Service
@RequiredArgsConstructor
public class FeeStandardService {

    private final BizFeeStandardMapper feeStandardMapper;
    private final OperationLogService operationLogService;

    public List<BizFeeStandard> list() {
        QueryWrapper<BizFeeStandard> query = new QueryWrapper<>();
        query.eq("is_active", 1)
                .orderByAsc("dept_code", "platform", "category", "rule_key");
        return feeStandardMapper.selectList(query);
    }

    public BigDecimal value(String deptCode, String platform, String category,
                            String ruleKey, BigDecimal fallback) {
        QueryWrapper<BizFeeStandard> query = new QueryWrapper<>();
        query.eq("dept_code", normalize(deptCode))
                .eq("platform", normalize(platform))
                .eq("category", normalize(category))
                .eq("rule_key", ruleKey)
                .eq("is_active", 1)
                .last("LIMIT 1");
        BizFeeStandard standard = feeStandardMapper.selectOne(query);
        return standard != null && standard.getRuleValue() != null ? standard.getRuleValue() : fallback;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FeeStandardUpdateDTO dto, Long operatorId,
                       String operatorName, String operatorRole, String ipAddress) {
        BizFeeStandard existing = feeStandardMapper.selectById(id);
        if (existing == null || !Integer.valueOf(1).equals(existing.getIsActive())) {
            throw ApiException.badRequest("稿费标准不存在或已停用");
        }
        BizFeeStandard update = new BizFeeStandard();
        update.setId(id);
        update.setRuleValue(dto.getRuleValue());
        update.setDescription(dto.getDescription());
        update.setRemark(dto.getRemark());
        feeStandardMapper.updateById(update);
        operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                "FEE_STANDARD", id,
                String.format("修改稿费标准 %s/%s/%s/%s: %s -> %s",
                        existing.getDeptCode(), existing.getPlatform(), existing.getCategory(),
                        existing.getRuleKey(), existing.getRuleValue(), dto.getRuleValue()),
                ipAddress);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
