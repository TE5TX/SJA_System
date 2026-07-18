package com.sja.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 个人资料修改 DTO。
 * 除 username（账号，自动跟随学号）和 password（密码，走独立接口）外，
 * 其余用户字段均可通过此 DTO 修改。
 */
@Data
public class ProfileUpdateDTO {

    /** 真实姓名 */
    @NotBlank(message = "姓名不能为空")
    private String realName;

    /** 性别：0-未知，1-男，2-女 */
    @NotNull(message = "性别不能为空")
    private Integer gender;

    /** 照片URL */
    private String photo;

    /** 身份证号 */
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    /** 学号（修改时会自动同步更新 username 账号） */
    @NotBlank(message = "学号不能为空")
    private String studentNo;

    /** 二级学院 */
    @NotBlank(message = "二级学院不能为空")
    private String college;

    /** 班级 */
    @NotBlank(message = "班级不能为空")
    private String className;

    /** 宿舍地址 */
    @NotBlank(message = "宿舍地址不能为空")
    private String dormAddress;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 银行卡号 */
    @NotBlank(message = "银行卡号不能为空")
    private String bankCard;

}
