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

    /**
     * 未发布作品入库后，将隐藏的发布日期设为该任务实际创建日期。
     *
     * <p>202609281554 - 完成了未发布任务按数据库创建日期填写隐藏发布日期的持久化能力。</p>
     *
     * @param workId 已存在的作品主键
     */
    default void assignCreationDateToUnpublishedWork(Long workId) {
        // 月度补贴按补贴月份归属，不使用作品发布日期。
    }

    void updateTotal(Long workId, BigDecimal totalAmount, int contributorCount);

    Map<String, Object> preserveRestrictedFields(Long workId, Map<String, Object> submittedInfo);

    boolean existsDuplicate(Map<String, Object> info,
                            List<FeeSubmitDTO.ContributorDTO> contributors,
                            Long excludedId);
}
