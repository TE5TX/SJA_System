package com.sja.system.dto;

import lombok.Data;

/**
 * 个人资料修改 DTO。
 * 除 username（账号，自动跟随学号）和 password（密码，走独立接口）外，
 * 其余用户字段均可通过此 DTO 修改。
 */
@Data
public class ProfileUpdateDTO {

    /** 真实姓名 */
    private String realName;

    /** 性别：0-未知，1-男，2-女 */
    private Integer gender;

    /** 照片URL */
    private String photo;

    /** 身份证号 */
    private String idCard;

    /** 学号（修改时会自动同步更新 username 账号） */
    private String studentNo;

    /** 二级学院 */
    private String college;

    /** 班级 */
    private String className;

    /** 宿舍地址 */
    private String dormAddress;

    /** 手机号 */
    private String phone;

    /** 银行卡号 */
    private String bankCard;

}
