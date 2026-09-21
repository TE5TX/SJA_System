package com.sja.system.application.dto.user;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class UserUpdateDTO {

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

    @Size(max = 255, message = "照片地址不能超过 255 个字符")
    private String photo;

    @Size(max = 18, message = "身份证号不能超过 18 个字符")
    private String idCard;

    @Size(max = 20, message = "手机号不能超过 20 个字符")
    private String phone;

    @Size(max = 100, message = "学院不能超过 100 个字符")
    private String college;

    @Size(max = 50, message = "班级不能超过 50 个字符")
    private String className;

    @Size(max = 100, message = "宿舍地址不能超过 100 个字符")
    private String dormAddress;

    @Size(max = 30, message = "银行卡号不能超过 30 个字符")
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

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
