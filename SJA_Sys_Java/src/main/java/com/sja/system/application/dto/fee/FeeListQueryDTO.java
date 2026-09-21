package com.sja.system.application.dto.fee;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 稿费列表查询条件。
 *
 * <p>用于集中承载筛选条件和分页参数，避免 Controller 与应用服务之间传递过长的参数列表。</p>
 */
@Data
// 202609211059 - 完成了稿费列表零散查询参数的统一封装。
public class FeeListQueryDTO {

    private String deptCode;
    private String month;
    private String status;
    private String contributorName;
    private String createDate;
    private String publishStartDate;
    private String publishEndDate;
    private String createStartDate;
    private String createEndDate;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 200, message = "每页数量不能超过200")
    private Integer pageSize = 10;
}
