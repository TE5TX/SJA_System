package com.sja.system.application.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request used to consume a one-time user import preview token. */
// 202608210907 - 增加了批量注册确认请求的令牌格式校验。
@Data
public class UserImportConfirmDTO {

    @NotBlank(message = "导入确认令牌不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{32,128}$", message = "导入确认令牌格式不正确")
    private String importToken;

    @NotBlank(message = "请输入当前用户密码")
    @Size(max = 50, message = "当前用户密码格式不正确")
    private String currentPassword;
}
