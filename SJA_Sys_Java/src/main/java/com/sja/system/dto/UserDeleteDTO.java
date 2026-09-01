package com.sja.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Current-login password confirmation for permanently deleting a managed user.
 */
@Data
public class UserDeleteDTO {

    @NotBlank(message = "请输入当前登录账号密码")
    @Size(max = 50, message = "当前登录账号密码不能超过50个字符")
    private String currentPassword;
}
