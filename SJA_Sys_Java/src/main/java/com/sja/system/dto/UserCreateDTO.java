package com.sja.system.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class UserCreateDTO {

    @Size(max = 50, message = "姓名不能超过 50 个字符")
    private String realName;

    @Min(value = 0, message = "性别参数不正确")
    @Max(value = 2, message = "性别参数不正确")
    private Integer gender;

    @NotBlank(message = "学号不能为空")
    @Size(max = 20, message = "学号不能超过 20 个字符")
    private String studentNo;

    @Size(max = 50, message = "初始密码不能超过 50 个字符")
    private String password;

    @Size(max = 255, message = "照片地址不能超过 255 个字符")
    private String photo;

    @Pattern(regexp = "^$|^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 100, message = "学院不能超过 100 个字符")
    private String college;

    @Size(max = 50, message = "班级不能超过 50 个字符")
    private String className;

    @Size(max = 100, message = "宿舍地址不能超过 100 个字符")
    private String dormAddress;

    @Pattern(regexp = "^$|^\\d{10,30}$", message = "银行卡号格式不正确")
    private String bankCard;

    @Size(max = 10, message = "部门数量不能超过 10 个")
    private List<Long> deptIds;

    @Size(max = 20, message = "角色数量不能超过 20 个")
    private List<Long> roleIds;

    @Min(value = 0, message = "账号状态只能为 0 或 1")
    @Max(value = 1, message = "账号状态只能为 0 或 1")
    private Integer isValid;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
