package com.sja.system.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserExportDTO extends UserQueryDTO {

    @NotEmpty(message = "请至少选择一个导出字段")
    private List<String> fields;
}
