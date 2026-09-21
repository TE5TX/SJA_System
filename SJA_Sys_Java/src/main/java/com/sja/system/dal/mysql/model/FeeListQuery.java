package com.sja.system.dal.mysql.model;

import com.sja.system.application.dto.fee.FeeTaskRefDTO;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 稿费列表统一查询条件。
 *
 * <p>该对象仅在 Service 与 Mapper 之间传递查询参数，避免将权限和筛选规则散落到
 * 多个作品表查询中。</p>
 */
@Data
public class FeeListQuery {

    /** 当前操作者可访问的稿费类型。 */
    private Set<String> workTypes = Collections.emptySet();

    /** 采编、摄影等有发布平台的作品可按平台过滤。 */
    private String platform;

    private String month;

    private LocalDate monthStart;

    private LocalDate monthEnd;

    private LocalDate publishStart;

    private LocalDate publishEnd;

    private String publishStartMonth;

    private String publishEndMonth;

    private LocalDateTime createStart;

    private LocalDateTime createEndExclusive;

    /** 审核状态，或特殊值 UNPUBLISHED。 */
    private String status;

    private String contributorName;

    /** 批量导出时限定的任务集合，空值表示不限定。 */
    private List<FeeTaskRefDTO> selectedTasks = Collections.emptyList();
}
