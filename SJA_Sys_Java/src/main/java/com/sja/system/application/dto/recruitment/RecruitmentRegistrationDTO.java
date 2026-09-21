package com.sja.system.application.dto.recruitment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruitmentRegistrationDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过 50 个字符")
    private String realName;

    @NotBlank(message = "学号不能为空")
    @Size(max = 20, message = "学号不能超过 20 个字符")
    private String studentNo;

    // 202609011534 - 注册人员自行设置密码，并在服务层二次确认两次输入一致。
    @NotBlank(message = "请设置登录密码")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,50}$",
            message = "密码应为 8 到 50 位，且同时包含字母和数字")
    private String password;

    @NotBlank(message = "请再次输入登录密码")
    @Size(max = 50, message = "确认密码不能超过 50 个字符")
    private String confirmPassword;

    @NotNull(message = "请选择性别")
    @Min(value = 1, message = "性别参数不正确")
    @Max(value = 2, message = "性别参数不正确")
    private Integer gender;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "二级学院不能为空")
    @Size(max = 100, message = "二级学院不能超过 100 个字符")
    private String college;

    @NotBlank(message = "班级不能为空")
    @Size(max = 50, message = "班级不能超过 50 个字符")
    private String className;

    @NotBlank(message = "宿舍地址不能为空")
    @Size(max = 100, message = "宿舍地址不能超过 100 个字符")
    private String dormAddress;

    @Pattern(regexp = "^$|^\\d{10,30}$", message = "银行卡号格式不正确")
    private String bankCard;

    @NotBlank(message = "请选择所属部门")
    @Pattern(regexp = "^(EDITORIAL|PHOTOGRAPHY|VISUAL|VIDEO)$", message = "所属部门不正确")
    private String deptCode;

    @NotBlank(message = "请先完成人机验证")
    @Size(max = 512, message = "验证码参数不正确")
    private String captchaToken;
}
