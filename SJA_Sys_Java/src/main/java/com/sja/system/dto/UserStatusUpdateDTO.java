package com.sja.system.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class UserStatusUpdateDTO {

    @NotNull(message = "缺少 status 参数")
    @Min(value = 0, message = "账号状态只能为 0 或 1")
    @Max(value = 1, message = "账号状态只能为 0 或 1")
    private Integer status;
}
