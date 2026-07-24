package com.sja.system.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class UserBatchUpdateDTO {

    @NotEmpty(message = "请至少选择一名人员")
    private List<Long> userIds;

    private List<Long> roleIds;

    @Min(value = 0, message = "账号状态只能为 0 或 1")
    @Max(value = 1, message = "账号状态只能为 0 或 1")
    private Integer isValid;
}
