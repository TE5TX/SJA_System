package com.sja.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private Integer mustChangePassword;

    private Integer tokenVersion;

    private String realName;

    private Integer gender;

    private String photo;

    private String idCard;

    private String studentNo;

    private String college;

    private String className;

    private String dormAddress;

    private String phone;

    private String bankCard;

    private Integer isValid;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
