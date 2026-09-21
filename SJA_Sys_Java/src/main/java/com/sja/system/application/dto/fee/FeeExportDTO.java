package com.sja.system.application.dto.fee;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class FeeExportDTO {

    private String deptCode;
    private String month;
    // 202609091535 - 完成了导出与列表共用创建日期筛选条件的功能。
    private String createDate;
    // 202609091610 - 完成了导出发布日期、创建日期范围条件的接入（含结束当天）。
    private String publishStartDate;
    private String publishEndDate;
    private String createStartDate;
    private String createEndDate;
    private String status;

    private String contributorName;

    private String templateType;

    @NotEmpty(message = "导出字段不能为空")
    private List<String> fields;

    @Size(max = 200, message = "单次最多选择200个稿费任务导出")
    private List<@Valid FeeTaskRefDTO> selectedTasks;

}
