package com.sja.system.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class FeeExportDTO {

    private String deptCode;
    private String month;
    private String status;

    private String contributorName;

    @NotEmpty(message = "导出字段不能为空")
    private List<String> fields;

    @Size(max = 200, message = "单次最多选择200个稿费任务导出")
    private List<@Valid FeeTaskRefDTO> selectedTasks;
}
