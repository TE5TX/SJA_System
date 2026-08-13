package com.sja.system.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class UserCreateDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过 50 个字符")
    private String realName;

    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别参数不正确")
    @Max(value = 2, message = "性别参数不正确")
    private Integer gender;

    @NotBlank(message = "学号不能为空")
    @Size(max = 20, message = "学号不能超过 20 个字符")
    private String studentNo;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 50, message = "初始密码长度应为 6 到 50 个字符")
    private String password;

    @Size(max = 255, message = "照片地址不能超过 255 个字符")
    private String photo;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "学院不能为空")
    @Size(max = 100, message = "学院不能超过 100 个字符")
    private String college;

    @NotBlank(message = "班级不能为空")
    @Size(max = 50, message = "班级不能超过 50 个字符")
    private String className;

    @NotBlank(message = "宿舍地址不能为空")
    @Size(max = 100, message = "宿舍地址不能超过 100 个字符")
    private String dormAddress;

    @NotBlank(message = "银行卡号不能为空")
    @Pattern(regexp = "^\\d{10,30}$", message = "银行卡号格式不正确")
    private String bankCard;

    @NotEmpty(message = "请至少选择一个部门")
    @Size(max = 10, message = "部门数量不能超过 10 个")
    private List<@NotNull(message = "部门ID不能为空") Long> deptIds;

    @NotEmpty(message = "请至少选择一个角色")
    @Size(max = 20, message = "角色数量不能超过 20 个")
    private List<@NotNull(message = "角色ID不能为空") Long> roleIds;

    @NotNull(message = "账号状态不能为空")
    @Min(value = 0, message = "账号状态只能为 0 或 1")
    @Max(value = 1, message = "账号状态只能为 0 或 1")
    private Integer isValid;

    @NotBlank(message = "备注不能为空")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
