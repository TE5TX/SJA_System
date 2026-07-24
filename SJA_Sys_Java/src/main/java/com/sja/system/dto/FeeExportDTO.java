package com.sja.system.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class FeeExportDTO {

    private String deptCode;
    private String month;
    private String status;

    @NotEmpty(message = "导出字段不能为空")
    private List<String> fields;
}
