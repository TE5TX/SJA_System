package com.sja.system.service.fee;

import com.sja.system.application.dto.fee.FeeSubmitDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 单一作品类型的数据持久化适配端口。
 */
// 202609201644 - 定义了五类稿费作品统一的创建、更新和查重能力。
public interface FeeWorkAdapter {

    FeeWorkType supports();

    Long create(Map<String, Object> info, Long submitterId);

    void update(Long workId, Map<String, Object> info);

    void updateTotal(Long workId, BigDecimal totalAmount, int contributorCount);

    Map<String, Object> preserveRestrictedFields(Long workId, Map<String, Object> submittedInfo);

    boolean existsDuplicate(Map<String, Object> info,
                            List<FeeSubmitDTO.ContributorDTO> contributors,
                            Long excludedId);
}
