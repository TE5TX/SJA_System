package com.sja.system.application.dto.user;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class UserBatchUpdateDTO {

    @NotEmpty(message = "请至少选择一名人员")
    private List<Long> userIds;

    private List<Long> roleIds;

    @Min(value = 0, message = "账号状态只能为 0 或 1")
    @Max(value = 1, message = "账号状态只能为 0 或 1")
    private Integer isValid;

    @NotBlank(message = "请输入当前用户密码")
    @Size(max = 50, message = "当前用户密码不能超过50个字符")
    private String currentPassword;
}
